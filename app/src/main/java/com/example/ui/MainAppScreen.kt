package com.example.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.model.DeviceContact
import com.example.model.TariffPlan
import com.example.system.NetworkStatus
import com.example.system.SystemBridgeHelper
import com.example.ui.components.CallingBridgeOverlay
import com.example.ui.components.ContactsPickerSheet
import com.example.ui.screens.ContractScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TariffsScreen
import kotlinx.coroutines.launch

enum class KMobileTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DIALER("dialer", R.string.tab_dialer, Icons.Filled.Call, Icons.Outlined.Call),
    TARIFFS("tariffs", R.string.tab_tariffs, Icons.Filled.SimCard, Icons.Outlined.SimCard),
    CONTRACT("contract", R.string.tab_contract, Icons.Filled.Description, Icons.Outlined.Description),
    HISTORY("history", R.string.tab_history, Icons.Filled.History, Icons.Outlined.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: KMobileViewModel,
    userDisplayName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val contractState by viewModel.contractState.collectAsStateWithLifecycle()
    val callHistoryState by viewModel.callHistoryState.collectAsStateWithLifecycle()
    val dialInput by viewModel.dialInput.collectAsStateWithLifecycle()
    val selectedContactName by viewModel.selectedContactName.collectAsStateWithLifecycle()
    val availableNumbers by viewModel.availableNumbers.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val activeBridge by viewModel.activeBridge.collectAsStateWithLifecycle()

    val networkStatus by SystemBridgeHelper.observeNetworkStatus(context)
        .collectAsStateWithLifecycle(initialValue = NetworkStatus(true, "Internet"))

    var currentTab by rememberSaveable { mutableStateOf(KMobileTab.DIALER) }
    var preselectedTariff by rememberSaveable { mutableStateOf(TariffPlan.FREE_0) }
    var permissionsList by remember {
        mutableStateOf(SystemBridgeHelper.checkPermissionsState(context))
    }
    var hasPromptedPermissionsOnLaunch by rememberSaveable { mutableStateOf(false) }

    var showContactsSheet by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }

    val refreshPermissionsState = {
        permissionsList = SystemBridgeHelper.checkPermissionsState(context)
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshPermissionsState()
        if (results[Manifest.permission.READ_CONTACTS] == true && showContactsSheet) {
            scope.launch {
                deviceContacts = SystemBridgeHelper.loadDeviceContacts(context)
            }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissionsState()
        if (granted) {
            scope.launch {
                deviceContacts = SystemBridgeHelper.loadDeviceContacts(context)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissionsState()
    }

    // Request Android system permissions proactively once after entering the app
    LaunchedEffect(Unit) {
        if (!hasPromptedPermissionsOnLaunch) {
            hasPromptedPermissionsOnLaunch = true
            val missing = SystemBridgeHelper.requiredRuntimePermissions().filter { perm ->
                permissionsList.any { it.permission == perm && !it.isGranted }
            }
            if (missing.isNotEmpty()) {
                permissionsLauncher.launch(missing.toTypedArray())
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (activeBridge != null) {
        BackHandler { viewModel.cancelActiveBridge() }
    } else if (currentTab != KMobileTab.DIALER) {
        BackHandler { currentTab = KMobileTab.DIALER }
    }

    val hasRequiredCallPermissions =
        SystemBridgeHelper.hasCallPhonePermission(context) &&
            SystemBridgeHelper.hasAudioPermission(context)

    val requestAllMissingPermissions: () -> Unit = {
        val perms = SystemBridgeHelper.requiredRuntimePermissions().toTypedArray()
        permissionsLauncher.launch(perms)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar {
                            KMobileTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail {
                            KMobileTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("rail_tab_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            KMobileTab.DIALER -> {
                                DialerScreen(
                                    contractState = contractState,
                                    dialInput = dialInput,
                                    selectedContactName = selectedContactName,
                                    networkStatus = networkStatus,
                                    hasRequiredCallPermissions = hasRequiredCallPermissions,
                                    onDigitPress = viewModel::appendDigit,
                                    onBackspace = viewModel::backspaceDigit,
                                    onClearInput = viewModel::clearDialInput,
                                    onOpenContacts = {
                                        showContactsSheet = true
                                        if (SystemBridgeHelper.hasContactsPermission(context)) {
                                            scope.launch {
                                                deviceContacts = SystemBridgeHelper.loadDeviceContacts(context)
                                            }
                                        }
                                    },
                                    onRequestPermissions = requestAllMissingPermissions,
                                    onOpenContractTab = { currentTab = KMobileTab.CONTRACT },
                                    onPlaceCall = { activeContract ->
                                        if (!SystemBridgeHelper.hasCallPhonePermission(context)) {
                                            requestAllMissingPermissions()
                                            viewModel.notifyMessage(
                                                "Разрешите телефонные звонки для перехода в настоящий вызов."
                                            )
                                        }
                                        viewModel.startInternetToRealCall(
                                            contract = activeContract,
                                            rawRecipient = dialInput,
                                            recipientName = selectedContactName,
                                            networkType = networkStatus.transportLabel,
                                            isInternetAvailable = networkStatus.isConnected,
                                            onLaunchRealCall = { normalizedNumber ->
                                                val launched = SystemBridgeHelper.launchRealAndroidCall(
                                                    context = context,
                                                    phoneNumber = normalizedNumber
                                                )
                                                if (!launched) {
                                                    viewModel.notifyMessage(
                                                        "На устройстве не найдено приложение для звонков."
                                                    )
                                                }
                                            }
                                        )
                                    }
                                )
                            }

                            KMobileTab.TARIFFS -> {
                                TariffsScreen(
                                    contractState = contractState,
                                    isSubmitting = isSubmitting,
                                    onSwitchTariff = viewModel::switchTariff,
                                    onOpenContractForTariff = { tariff ->
                                        preselectedTariff = tariff
                                        currentTab = KMobileTab.CONTRACT
                                    }
                                )
                            }

                            KMobileTab.CONTRACT -> {
                                ContractScreen(
                                    contractState = contractState,
                                    defaultUserName = userDisplayName,
                                    defaultUserEmail = userEmail,
                                    availableNumbers = availableNumbers,
                                    preselectedTariff = preselectedTariff,
                                    isSubmitting = isSubmitting,
                                    permissions = permissionsList,
                                    onRefreshNumbers = viewModel::refreshVirtualNumbers,
                                    onSignContract = { name, doc, email, num, tariff, strokeCount, json ->
                                        viewModel.signContract(
                                            fullName = name,
                                            documentIdNumber = doc,
                                            contactEmail = email,
                                            virtualNumber = num,
                                            tariff = tariff,
                                            signatureStrokeCount = strokeCount,
                                            signaturePointsJson = json,
                                            onSuccess = { currentTab = KMobileTab.DIALER }
                                        )
                                    },
                                    onTerminateContract = viewModel::terminateContract,
                                    onRequestPermissions = requestAllMissingPermissions,
                                    onOpenSettings = { SystemBridgeHelper.openAppPermissionsSettings(context) },
                                    onSignOut = {
                                        signOut(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }

                            KMobileTab.HISTORY -> {
                                HistoryScreen(
                                    callsState = callHistoryState,
                                    onRedial = { phone, name ->
                                        viewModel.selectRecipient(phone, name)
                                        currentTab = KMobileTab.DIALER
                                    },
                                    onDeleteCall = viewModel::deleteCall
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showContactsSheet) {
            ContactsPickerSheet(
                contacts = deviceContacts,
                hasContactsPermission = SystemBridgeHelper.hasContactsPermission(context),
                onRequestContactsPermission = {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                onSelectContact = { contact ->
                    viewModel.selectRecipient(contact.phoneNumber, contact.name)
                    showContactsSheet = false
                },
                onDismiss = { showContactsSheet = false }
            )
        }

        activeBridge?.let { bridge ->
            CallingBridgeOverlay(
                bridge = bridge,
                onCancel = viewModel::cancelActiveBridge
            )
        }
    }
}
