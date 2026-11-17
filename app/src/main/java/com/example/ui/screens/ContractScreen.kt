package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SubscriberContract
import com.example.model.TariffPlan
import com.example.model.UiState
import com.example.model.formatRussianPhone
import com.example.system.SystemPermissionItem
import com.example.ui.components.PermissionsStatusCard
import com.example.ui.components.SignaturePadCanvas
import com.example.ui.components.SignaturePreviewCanvas
import com.example.ui.components.serializeSignatureStrokes
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ContractScreen(
    contractState: UiState<SubscriberContract?>,
    defaultUserName: String,
    defaultUserEmail: String,
    availableNumbers: List<String>,
    preselectedTariff: TariffPlan,
    isSubmitting: Boolean,
    permissions: List<SystemPermissionItem>,
    onRefreshNumbers: () -> Unit,
    onSignContract: (
        fullName: String,
        documentId: String,
        email: String,
        virtualNumber: String,
        tariff: TariffPlan,
        strokeCount: Int,
        signaturePointsJson: String
    ) -> Unit,
    onTerminateContract: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    var forceFormMode by rememberSaveable { mutableStateOf(false) }

    val loadedContract = (contractState as? UiState.Success)?.data
    val showActiveView = loadedContract != null && loadedContract.activeContract() && !forceFormMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (contractState is UiState.Loading) {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Загрузка данных договора К mobile…")
                    }
                }
            } else if (showActiveView && loadedContract != null) {
                ActiveContractCard(
                    contract = loadedContract,
                    isSubmitting = isSubmitting,
                    onResign = { forceFormMode = true },
                    onTerminate = onTerminateContract
                )
            } else {
                ContractSigningForm(
                    existingContract = loadedContract,
                    defaultUserName = defaultUserName,
                    defaultUserEmail = defaultUserEmail,
                    availableNumbers = availableNumbers,
                    preselectedTariff = preselectedTariff,
                    isSubmitting = isSubmitting,
                    showCancelToActive = loadedContract?.activeContract() == true && forceFormMode,
                    onCancelToActive = { forceFormMode = false },
                    onRefreshNumbers = onRefreshNumbers,
                    onSubmit = { name, doc, email, num, tariff, strokesCount, json ->
                        onSignContract(name, doc, email, num, tariff, strokesCount, json)
                        forceFormMode = false
                    }
                )
            }

            PermissionsStatusCard(
                permissions = permissions,
                onRequestPermissions = onRequestPermissions,
                onOpenSettings = onOpenSettings
            )

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auth_sign_out))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActiveContractCard(
    contract: SubscriberContract,
    isSubmitting: Boolean,
    onResign: () -> Unit,
    onTerminate: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")) }
    val signedDate = contract.updatedAt?.toDate()?.let { dateFormat.format(it) } ?: "Сегодня"

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Договор ${contract.contractNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = JetBrainsMonoFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.contract_status_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            ContractInfoRow("Оператор", "ООО «К mobile»")
            ContractInfoRow("Виртуальный номер", formatRussianPhone(contract.virtualNumber), mono = true)
            ContractInfoRow(
                "Тарифный план",
                "${contract.tariffPlan().title} (${contract.tariffPriceRub} ₽/мес)"
            )
            ContractInfoRow("Абонент", contract.fullName)
            ContractInfoRow("Документ", contract.documentIdNumber, mono = true)
            ContractInfoRow("Контактный e-mail", contract.contactEmail)
            ContractInfoRow("Дата оформления", signedDate)

            Text(
                text = "Электронно-графическая подпись абонента:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SignaturePreviewCanvas(serializedStrokes = contract.signaturePointsJson)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResign,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.contract_resign_button))
                }
                OutlinedButton(
                    onClick = onTerminate,
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("terminate_contract_button")
                ) {
                    Text(stringResource(R.string.contract_terminate_button))
                }
            }
        }
    }
}

@Composable
private fun ContractInfoRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContractSigningForm(
    existingContract: SubscriberContract?,
    defaultUserName: String,
    defaultUserEmail: String,
    availableNumbers: List<String>,
    preselectedTariff: TariffPlan,
    isSubmitting: Boolean,
    showCancelToActive: Boolean,
    onCancelToActive: () -> Unit,
    onRefreshNumbers: () -> Unit,
    onSubmit: (
        fullName: String,
        documentId: String,
        email: String,
        virtualNumber: String,
        tariff: TariffPlan,
        strokeCount: Int,
        signaturePointsJson: String
    ) -> Unit
) {
    var fullName by rememberSaveable {
        mutableStateOf(existingContract?.fullName?.ifBlank { defaultUserName } ?: defaultUserName)
    }
    var documentId by rememberSaveable {
        mutableStateOf(existingContract?.documentIdNumber ?: "")
    }
    var email by rememberSaveable {
        mutableStateOf(existingContract?.contactEmail?.ifBlank { defaultUserEmail } ?: defaultUserEmail)
    }
    var selectedNumber by rememberSaveable(availableNumbers) {
        mutableStateOf(
            existingContract?.virtualNumber?.takeIf { it.startsWith("+7990") }
                ?: availableNumbers.firstOrNull()
                ?: "+79901007700"
        )
    }
    var selectedTariff by rememberSaveable { mutableStateOf(preselectedTariff) }
    var termsExpanded by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.contract_form_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (showCancelToActive) {
                    TextButton(onClick = onCancelToActive) {
                        Text("Назад")
                    }
                }
            }

            if (existingContract?.status == SubscriberContract.CONTRACT_STATUS_TERMINATED) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Предыдущий договор был расторгнут. Оформите новый договор для доступа к звонкам.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; validationError = null },
                label = { Text(stringResource(R.string.contract_field_full_name)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contract_full_name_input")
            )

            OutlinedTextField(
                value = documentId,
                onValueChange = { documentId = it; validationError = null },
                label = { Text(stringResource(R.string.contract_field_document)) },
                placeholder = { Text("Например: 4512 908765") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contract_document_input")
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; validationError = null },
                label = { Text(stringResource(R.string.contract_field_email)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contract_email_input")
            )

            // Virtual +7990 Number Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.contract_pick_number),
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = {
                        onRefreshNumbers()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.contract_refresh_numbers))
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableNumbers.forEach { number ->
                    FilterChip(
                        selected = selectedNumber == number,
                        onClick = { selectedNumber = number },
                        label = {
                            Text(
                                text = formatRussianPhone(number),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // Tariff Selection
            Text(
                text = stringResource(R.string.contract_pick_tariff),
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TariffPlan.entries.forEach { tariff ->
                    val selected = selectedTariff == tariff
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = if (selected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTariff = tariff }
                            .testTag("contract_tariff_${tariff.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tariff.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${tariff.priceRub} ₽ / мес",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Contract Terms Accordion
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { termsExpanded = !termsExpanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.contract_terms_title),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (termsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(visible = termsExpanded) {
                        Text(
                            text = stringResource(R.string.contract_terms_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { acceptedTerms = !acceptedTerms }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    modifier = Modifier.testTag("contract_accept_checkbox")
                )
                Text(
                    text = stringResource(R.string.contract_accept_checkbox),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SignaturePadCanvas(
                strokes = strokes,
                onAddStroke = { newStroke ->
                    strokes.add(newStroke)
                    validationError = null
                },
                onClear = { strokes.clear() }
            )

            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val serialized = serializeSignatureStrokes(strokes)
                    when {
                        fullName.trim().length < 2 ->
                            validationError = "Укажите ФИО абонента (минимум 2 символа)."
                        documentId.trim().length < 4 ->
                            validationError = "Укажите серию и номер документа (минимум 4 символа)."
                        email.trim().length < 3 || !email.contains("@") ->
                            validationError = "Укажите корректный e-mail."
                        !acceptedTerms ->
                            validationError = "Отметьте согласие с условиями договора К mobile."
                        strokes.isEmpty() || serialized.length < 2 ->
                            validationError = "Поставьте подпись пальцем в поле подписи."
                        else -> {
                            validationError = null
                            onSubmit(
                                fullName,
                                documentId,
                                email,
                                selectedNumber,
                                selectedTariff,
                                strokes.size,
                                serialized
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("sign_contract_submit_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Оформление договора…")
                } else {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.contract_sign_submit),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
