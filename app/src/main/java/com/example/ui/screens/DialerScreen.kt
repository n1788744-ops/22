package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SubscriberContract
import com.example.model.UiState
import com.example.model.formatRussianPhone
import com.example.system.NetworkStatus
import com.example.ui.components.VirtualSimHeroCard
import com.example.ui.theme.JetBrainsMonoFamily

private val DIALPAD_ROWS = listOf(
    listOf("1" to "", "2" to "АБВГ", "3" to "ДЕЖЗ"),
    listOf("4" to "ИЙКЛ", "5" to "МНОП", "6" to "РСТУ"),
    listOf("7" to "ФХЦЧ", "8" to "ШЩЪЫ", "9" to "ЬЭЮЯ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    contractState: UiState<SubscriberContract?>,
    dialInput: String,
    selectedContactName: String,
    networkStatus: NetworkStatus,
    hasRequiredCallPermissions: Boolean,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearInput: () -> Unit,
    onOpenContacts: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenContractTab: () -> Unit,
    onPlaceCall: (SubscriberContract) -> Unit
) {
    val activeContract = (contractState as? UiState.Success)?.data?.takeIf { it.activeContract() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VirtualSimHeroCard(
                contract = (contractState as? UiState.Success)?.data,
                networkStatus = networkStatus,
                onOpenContract = onOpenContractTab
            )

            // Mandatory contract gate when user has no active contract
            when (contractState) {
                is UiState.Loading -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Проверяем абонентский договор К mobile…")
                        }
                    }
                }

                is UiState.Error -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ошибка загрузки договора",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = contractState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (activeContract == null) {
                        ContractGateCard(onOpenContractTab = onOpenContractTab)
                    } else {
                        if (!hasRequiredCallPermissions) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Выдайте системные разрешения Android для звонков и микрофона.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = onRequestPermissions,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Выдать")
                                    }
                                }
                            }
                        }

                        // Dialer Number Display
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (selectedContactName.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = selectedContactName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilledTonalIconButton(
                                        onClick = onOpenContacts,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("open_contacts_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Contacts,
                                            contentDescription = stringResource(R.string.dialer_contacts_button)
                                        )
                                    }

                                    Text(
                                        text = if (dialInput.isEmpty()) {
                                            stringResource(R.string.dialer_enter_number)
                                        } else {
                                            formatRussianPhone(dialInput)
                                        },
                                        style = if (dialInput.isEmpty()) {
                                            MaterialTheme.typography.titleMedium
                                        } else {
                                            MaterialTheme.typography.headlineMedium
                                        },
                                        fontFamily = if (dialInput.isEmpty()) {
                                            MaterialTheme.typography.titleMedium.fontFamily
                                        } else {
                                            JetBrainsMonoFamily
                                        },
                                        color = if (dialInput.isEmpty()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = onBackspace,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .combinedClickable(
                                                onClick = onBackspace,
                                                onLongClick = onClearInput
                                            )
                                            .testTag("dialer_backspace_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Стереть цифру",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Numeric Keypad
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DIALPAD_ROWS.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { (digit, letters) ->
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 2.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .combinedClickable(
                                                    onClick = { onDigitPress(digit) },
                                                    onLongClick = {
                                                        if (digit == "0") onDigitPress("+")
                                                    }
                                                )
                                                .testTag("dial_key_$digit")
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = digit,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = JetBrainsMonoFamily,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (letters.isNotEmpty()) {
                                                    Text(
                                                        text = letters,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Internet -> Real Call CTA Button
                        Button(
                            onClick = { onPlaceCall(activeContract) },
                            enabled = dialInput.filter { it.isDigit() }.length >= 3,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("dial_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiCalling3,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.dialer_call_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (networkStatus.isConnected) {
                                "Сеть ${networkStatus.transportLabel}: вызов с ${formatRussianPhone(activeContract.virtualNumber)} перейдёт в настоящий звонок Android"
                            } else {
                                "Нет интернет-сети: подключите Wi-Fi или мобильный интернет для звонка"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractGateCard(onOpenContractTab: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.contract_required_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.contract_required_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenContractTab,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("gate_open_contract_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.contract_cta_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
