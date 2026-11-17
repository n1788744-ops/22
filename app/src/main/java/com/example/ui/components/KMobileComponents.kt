package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceContact
import com.example.model.SubscriberContract
import com.example.model.formatRussianPhone
import com.example.system.NetworkStatus
import com.example.system.SystemPermissionItem
import com.example.ui.CallBridgeState
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

@Composable
fun VirtualSimHeroCard(
    contract: SubscriberContract?,
    networkStatus: NetworkStatus,
    onOpenContract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = contract?.activeContract() == true
    val tariff = contract?.tariffPlan()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0C182D),
                            Color(0xFF13294B),
                            Color(0xFF0A323A)
                        )
                    )
                )
        ) {
            // Custom eSIM circuit & wave Canvas background
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    radius = h * 0.65f,
                    center = Offset(w * 0.92f, h * 0.15f)
                )
                drawCircle(
                    color = CobaltBlue.copy(alpha = 0.16f),
                    radius = h * 0.50f,
                    center = Offset(w * 0.72f, h * 0.95f)
                )
                val wave = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.30f, h * 0.58f, w * 0.65f, h * 0.95f, w, h * 0.62f)
                }
                drawPath(
                    path = wave,
                    color = ElectricCyan.copy(alpha = 0.28f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = "eSIM К mobile",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "К mobile eSIM",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Live internet connection chip
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (networkStatus.isConnected) {
                            ElectricCyan.copy(alpha = 0.18f)
                        } else {
                            CoralRed.copy(alpha = 0.22f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (networkStatus.isConnected) {
                                    Icons.Filled.Wifi
                                } else {
                                    Icons.Filled.SignalWifiOff
                                },
                                contentDescription = null,
                                tint = if (networkStatus.isConnected) ElectricCyan else CoralRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = networkStatus.transportLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                if (isActive && contract != null) {
                    Text(
                        text = formatRussianPhone(contract.virtualNumber),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Тариф: ${tariff?.title ?: "К mobile Старт"} (${contract.tariffPriceRub} ₽/мес)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Договор ${contract.contractNumber} • Активен",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onOpenContract)
                        ) {
                            Text(
                                text = "Договор",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+7 (990) XXX-XX-XX",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = stringResource(R.string.contract_required_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignaturePadCanvas(
    strokes: List<List<Offset>>,
    onAddStroke: (List<Offset>) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val strokeColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.contract_signature_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = {
                    activeStroke = emptyList()
                    onClear()
                },
                modifier = Modifier.testTag("clear_signature_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.contract_signature_clear))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            activeStroke = listOf(start)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activeStroke = activeStroke + change.position
                        },
                        onDragEnd = {
                            if (activeStroke.size >= 2) {
                                onAddStroke(activeStroke)
                            } else if (activeStroke.size == 1) {
                                val pt = activeStroke.first()
                                onAddStroke(listOf(pt, Offset(pt.x + 2f, pt.y + 2f)))
                            }
                            activeStroke = emptyList()
                        },
                        onDragCancel = {
                            activeStroke = emptyList()
                        }
                    )
                }
        ) {
            if (strokes.isEmpty() && activeStroke.isEmpty()) {
                Text(
                    text = "Нарисуйте подпись здесь",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val allStrokes = if (activeStroke.isNotEmpty()) strokes + listOf(activeStroke) else strokes
                allStrokes.forEach { pts ->
                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x, pts[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

fun serializeSignatureStrokes(strokes: List<List<Offset>>): String {
    if (strokes.isEmpty()) return ""
    return strokes.joinToString("|") { stroke ->
        stroke.take(40).joinToString(";") { pt ->
            "${pt.x.roundToInt()},${pt.y.roundToInt()}"
        }
    }.take(19500)
}

@Composable
fun SignaturePreviewCanvas(
    serializedStrokes: String,
    modifier: Modifier = Modifier
) {
    val parsedStrokes = remember(serializedStrokes) {
        serializedStrokes.split("|").mapNotNull { strokeStr ->
            val pts = strokeStr.split(";").mapNotNull { pair ->
                val parts = pair.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) Offset(x, y) else null
                } else null
            }
            if (pts.size >= 2) pts else null
        }
    }

    val strokeColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val allPts = parsedStrokes.flatten()
            if (allPts.isEmpty()) return@Canvas
            val minX = allPts.minOf { it.x }
            val maxX = (allPts.maxOf { it.x }).coerceAtLeast(minX + 1f)
            val minY = allPts.minOf { it.y }
            val maxY = (allPts.maxOf { it.y }).coerceAtLeast(minY + 1f)
            val scaleX = size.width / (maxX - minX)
            val scaleY = size.height / (maxY - minY)
            val scale = minOf(scaleX, scaleY)

            parsedStrokes.forEach { stroke ->
                val path = Path().apply {
                    val first = stroke.first()
                    moveTo((first.x - minX) * scale, (first.y - minY) * scale)
                    for (i in 1 until stroke.size) {
                        lineTo((stroke[i].x - minX) * scale, (stroke[i].y - minY) * scale)
                    }
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun PermissionsStatusCard(
    permissions: List<SystemPermissionItem>,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGranted = permissions.all { it.isGranted }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permissions_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.permissions_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            permissions.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (item.isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (item.isGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!allGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("request_permissions_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.permissions_request_all))
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.permissions_open_settings),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.permissions_all_granted),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerSheet(
    contacts: List<DeviceContact>,
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onSelectContact: (DeviceContact) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(contacts, search) {
        if (search.isBlank()) contacts
        else contacts.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.phoneNumber.contains(search, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
                        imageVector = Icons.Filled.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Контакты устройства",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            if (!hasContactsPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Разрешите доступ к контактам, чтобы выбрать номер из телефонной книги Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Button(
                            onClick = onRequestContactsPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Выдать разрешение READ_CONTACTS")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Поиск по имени или номеру") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Контакты с телефонными номерами не найдены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { contact ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectContact(contact) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Позвонить",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallingBridgeOverlay(
    bridge: CallBridgeState,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian.copy(alpha = 0.94f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.25f * pulse),
                        radius = (size.minDimension / 2f) * pulse
                    )
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.5f),
                        radius = size.minDimension * 0.38f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = ElectricCyan,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = DeepObsidian,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = ElectricCyan.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "К mobile • ${bridge.networkType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Text(
                text = bridge.recipientName.ifBlank { formatRussianPhone(bridge.recipientNumber) },
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFamily,
                color = Color.White
            )

            Text(
                text = "Ваш номер: ${formatRussianPhone(bridge.callerVirtualNumber)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = JetBrainsMonoFamily,
                color = Color.White.copy(alpha = 0.78f)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { bridge.progress },
                    color = ElectricCyan,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Text(
                    text = bridge.phaseLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilledTonalButton(
                onClick = onCancel,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CoralRed.copy(alpha = 0.25f),
                    contentColor = Color.White
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.calling_bridge_cancel))
            }
        }
    }
}
