package za.co.farrusco.estellecherryq

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val Night = Color(0xFF020812)
private val Navy = Color(0xFF071426)
private val Cyan = Color(0xFF19D7FF)
private val Blue = Color(0xFF28A8FF)
private val Purple = Color(0xFFA72BFF)
private val Pink = Color(0xFFFF73D1)
private val Green = Color(0xFF12F469)
private val Red = Color(0xFFFF334A)
private val Orange = Color(0xFFFF9638)
private val Soft = Color(0xFF9FB7D9)
private val White = Color(0xFFF5FAFF)

@Composable
internal fun NeonAppShell(
    context: android.content.Context,
    tab: Tab,
    onTab: (Tab) -> Unit,
    status: RelayStatus,
    session: SessionState,
    history: List<ChargeRecord>,
    now: Long,
    busy: Boolean,
    baseUrl: String,
    token: String,
    ratePerKwh: Double,
    chargeAmps: Double,
    onRefresh: () -> Unit,
    onRelay1: (Boolean) -> Unit,
    onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: (String, String, Double, Double) -> Unit,
    onShare: () -> Unit
) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Cyan, background = Night, surface = Navy)) {
        Scaffold(
            containerColor = Night,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { BottomNavigation(tab, onTab) }
        ) { inner ->
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Night, Color(0xFF05142A), Night)))
                    .padding(inner)
            ) {
                Header()
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    when (tab) {
                        Tab.Home -> HomeScreen(status, session, now, onRefresh)
                        Tab.Control -> ControlScreen(status, session, now, busy, onRelay1, onRelay2, onStart, onStop)
                        Tab.History -> HistoryScreen(history)
                        Tab.Graphs -> GraphsScreen(history)
                        Tab.Settings -> SettingsScreen(baseUrl, token, status, session, now, ratePerKwh, chargeAmps, onSave, onRefresh)
                        Tab.Share -> ShareScreen(history, ratePerKwh, chargeAmps, onShare)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF260749), Color(0xFF0B1C4B), Color(0xFF073354))))
    ) {
        Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Purple.copy(.25f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ElectricCar, null, tint = Pink, modifier = Modifier.size(29.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Estelle Cherry Q", color = White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("Charging app • EF_1.3", color = Soft, fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Purple, Blue, Cyan))))
    }
}

@Composable
private fun NeonCard(colors: List<Color>, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(Brush.linearGradient(colors), RoundedCornerShape(19.dp)).padding(1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0D2340), Color(0xFF071322))), RoundedCornerShape(18.dp))
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun HomeScreen(status: RelayStatus, session: SessionState, now: Long, onRefresh: () -> Unit) {
    NeonCard(listOf(if (status.connected) Green else Red, Cyan)) {
        Text("System Status", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(if (status.connected) "Connected" else "Not connected", color = if (status.connected) Green else Red, fontWeight = FontWeight.ExtraBold)
        Text(if (status.connected) "Relay online • Home Wi-Fi" else status.message, color = Soft, fontSize = 12.sp)
        if (!status.connected) Text("Retry connection", color = Cyan, modifier = Modifier.clickable(onClick = onRefresh))
    }
    DeviceCard("Eskom Supply", "Relay 1", status.relay1On, listOf(Cyan, Blue))
    DeviceCard("EV Charger", "Relay 2", status.relay2On, listOf(Purple, Blue))
    SessionPanel(session, now)
}

@Composable
private fun DeviceCard(title: String, relay: String, on: Boolean, colors: List<Color>) {
    NeonCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(relay, color = Soft, fontSize = 12.sp)
                Text(if (on) "ACTIVE" else "OFF", color = if (on) Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (on) "ON" else "OFF", color = if (on) Green else Red, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun ControlScreen(
    status: RelayStatus, session: SessionState, now: Long, busy: Boolean,
    onRelay1: (Boolean) -> Unit, onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit, onStop: () -> Unit
) {
    RelayControl("Eskom Supply", "Relay 1", status.relay1On, status.connected && !busy, onRelay1, listOf(Cyan, Blue))
    RelayControl("EV Charger", "Relay 2", status.relay2On, status.connected && !busy, onRelay2, listOf(Purple, Blue))
    SessionPanel(session, now)
    val charging = status.relay1On && status.relay2On
    Button(
        onClick = if (charging) onStop else onStart,
        enabled = status.connected && !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = if (charging) Red else Green, contentColor = Night)
    ) { Text(if (charging) "STOP CHARGING" else "START CHARGING", fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun RelayControl(
    title: String, relay: String, on: Boolean, enabled: Boolean,
    setter: (Boolean) -> Unit, colors: List<Color>
) {
    NeonCard(colors) {
        Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(relay, color = Soft)
        Spacer(Modifier.height(7.dp))
        Button(
            onClick = { setter(!on) },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = if (on) Green else Red, contentColor = Night),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PowerSettingsNew, null)
            Spacer(Modifier.width(6.dp))
            Text(if (on) "ON" else "OFF", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SessionPanel(session: SessionState, now: Long) {
    val end = if (session.running) now else session.endedAt
    val duration = if (session.startedAt > 0 && end > 0) (end - session.startedAt).coerceAtLeast(0L) else 0L
    val kwh = estimateKwh(duration, session.amps)
    val cost = kwh * session.ratePerKwh
    NeonCard(listOf(Cyan, Purple)) {
        Text("Charging Session", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        SummaryRow("Started", if (session.startedAt > 0) timeText(session.startedAt) else "--:--")
        SummaryRow("Current time", timeText(now))
        SummaryRow("Elapsed", elapsedText(session, now))
        SummaryRow("Ended", if (!session.running && session.endedAt > 0) timeText(session.endedAt) else "--:--")
        if (session.startedAt > 0) {
            SummaryRow("Estimated energy", f2(kwh) + " kWh")
            SummaryRow("Estimated cost", "R" + f2(cost))
        }
    }
}

@Composable
private fun HistoryScreen(history: List<ChargeRecord>) {
    var filter by remember { mutableStateOf("All") }
    val now = System.currentTimeMillis()
    val filtered = history.filter {
        when (filter) {
            "7 days" -> it.startedAt >= now - 7L * 24 * 3600 * 1000
            "30 days" -> it.startedAt >= now - 30L * 24 * 3600 * 1000
            else -> true
        }
    }
    Title("History", Icons.Filled.History)
    Filters(listOf("7 days", "30 days", "All"), filter) { filter = it }
    NeonCard(listOf(Cyan, Blue)) {
        SummaryRow("Charges", filtered.size.toString())
        SummaryRow("Charging time", durationText(filtered.sumOf { it.durationMs }))
        SummaryRow("Estimated energy", f2(filtered.sumOf { it.estimatedKwh }) + " kWh")
        SummaryRow("Estimated spend", "R" + f2(filtered.sumOf { it.estimatedCost }))
        SummaryRow("Average current", if (filtered.isEmpty()) "--" else f1(filtered.map { it.amps }.average()) + " A")
    }
    if (filtered.isEmpty()) {
        NeonCard(listOf(Purple, Blue)) { Text("No completed charging sessions yet.", color = White) }
    } else {
        filtered.asReversed().forEach { r ->
            NeonCard(listOf(Purple, Cyan)) {
                Text(dateTimeText(r.startedAt), color = White, fontWeight = FontWeight.ExtraBold)
                Text("Ended " + timeText(r.endedAt) + " • " + durationText(r.durationMs), color = Soft, fontSize = 12.sp)
                SummaryRow("Current", f1(r.amps) + " A")
                SummaryRow("Tariff used", "R" + f2(r.ratePerKwh) + "/kWh")
                SummaryRow("Estimated energy", f3(r.estimatedKwh) + " kWh")
                SummaryRow("Estimated cost", "R" + f2(r.estimatedCost))
            }
        }
    }
}

@Composable
private fun GraphsScreen(history: List<ChargeRecord>) {
    Title("Graphs", Icons.Filled.BarChart)
    if (history.isEmpty()) {
        NeonCard(listOf(Cyan, Purple)) { Text("Graphs appear after completed charging sessions.", color = White) }
        return
    }
    NeonCard(listOf(Cyan, Blue)) {
        Text("ESTIMATED ENERGY BY SESSION", color = Soft, fontSize = 12.sp)
        BarChart(history.takeLast(7))
    }
    NeonCard(listOf(Blue, Purple)) {
        Text("ESTIMATED COST TREND", color = Soft, fontSize = 12.sp)
        LineChart(history.takeLast(12))
    }
    NeonCard(listOf(Purple, Cyan)) {
        Text("CHARGING TIME BY CURRENT", color = Soft, fontSize = 12.sp)
        PieChart(history)
    }
}

@Composable
private fun BarChart(records: List<ChargeRecord>) {
    Canvas(Modifier.fillMaxWidth().height(210.dp).padding(top = 10.dp)) {
        val m = max(0.1, records.maxOfOrNull { it.estimatedKwh } ?: 0.1)
        val gap = size.width / (records.size * 2f + 1f)
        records.forEachIndexed { i, r ->
            val h = ((r.estimatedKwh / m) * (size.height - 42f)).toFloat()
            val x = gap + i * gap * 2f
            drawRoundRect(Blue, Offset(x, size.height - 28f - h), Size(gap, h), androidx.compose.ui.geometry.CornerRadius(8f))
            val p = Paint().apply { color = android.graphics.Color.LTGRAY; textSize = 22f; textAlign = Paint.Align.CENTER }
            drawContext.canvas.nativeCanvas.drawText(SimpleDateFormat("dd", Locale.getDefault()).format(Date(r.startedAt)), x + gap / 2, size.height - 5f, p)
        }
    }
}

@Composable
private fun LineChart(records: List<ChargeRecord>) {
    Canvas(Modifier.fillMaxWidth().height(210.dp).padding(top = 10.dp)) {
        val m = max(0.1, records.maxOfOrNull { it.estimatedCost } ?: 0.1)
        var prev: Offset? = null
        records.forEachIndexed { i, r ->
            val x = if (records.size == 1) size.width / 2 else 12f + i * (size.width - 24f) / (records.size - 1)
            val y = size.height - 28f - ((r.estimatedCost / m) * (size.height - 48f)).toFloat()
            val pt = Offset(x, y)
            prev?.let { drawLine(Blue, it, pt, strokeWidth = 6f) }
            drawCircle(Cyan, 7f, pt)
            prev = pt
        }
    }
}

@Composable
private fun PieChart(records: List<ChargeRecord>) {
    val grouped = records.groupBy { f1(it.amps) }.mapValues { e -> e.value.sumOf { it.durationMs }.toDouble() }
    val total = grouped.values.sum().coerceAtLeast(1.0)
    val colors = listOf(Cyan, Purple, Green, Orange, Pink, Blue)
    Canvas(Modifier.fillMaxWidth().height(210.dp).padding(22.dp)) {
        var start = -90f
        grouped.entries.forEachIndexed { i, e ->
            val sweep = (e.value / total * 360).toFloat()
            drawArc(colors[i % colors.size], start, sweep, true, Offset((size.width - size.minDimension) / 2, 0f), Size(size.minDimension, size.minDimension))
            start += sweep
        }
    }
    grouped.entries.forEachIndexed { i, e ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape))
            Text(e.key + " A", color = White, modifier = Modifier.padding(start = 7.dp).weight(1f))
            Text(f1(e.value / total * 100) + "%", color = Soft)
        }
    }
}

@Composable
private fun SettingsScreen(
    baseUrl: String, token: String, status: RelayStatus, session: SessionState, now: Long,
    ratePerKwh: Double, chargeAmps: Double,
    onSave: (String, String, Double, Double) -> Unit, onTest: () -> Unit
) {
    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    var tokenText by remember(token) { mutableStateOf(token) }
    var rateText by remember(ratePerKwh) { mutableStateOf(f2(ratePerKwh)) }
    var ampsText by remember(chargeAmps) { mutableStateOf(f1(chargeAmps)) }

    Title("Settings", Icons.Filled.Settings)

    NeonCard(listOf(Cyan, Blue)) {
        Text("Device Connection", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        OutlinedTextField(urlText, { urlText = it }, label = { Text("Relay address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(tokenText, { tokenText = it }, label = { Text("Local control token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    }

    NeonCard(listOf(Purple, Cyan)) {
        Text("Charging Estimate Settings", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text("Voltage fixed at 230 V for the estimate.", color = Soft, fontSize = 12.sp)
        OutlinedTextField(rateText, { rateText = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Electricity rate (R/kWh)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(ampsText, { ampsText = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Charging current (A)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSave(urlText, tokenText, rateText.toDoubleOrNull() ?: ratePerKwh, ampsText.toDoubleOrNull() ?: chargeAmps)
            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Night)) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
            Button(onClick = onTest, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Night)) {
                Text("TEST", fontWeight = FontWeight.Bold)
            }
        }
    }

    NeonCard(listOf(if (status.connected) Green else Red, Cyan)) {
        Text(if (status.connected) "Connected" else "Not connected", color = if (status.connected) Green else Red, fontWeight = FontWeight.Bold)
        Text(if (status.connected) "Relay online • Home Wi-Fi" else status.message, color = Soft, fontSize = 12.sp)
    }
    SessionPanel(session, now)
}

@Composable
private fun ShareScreen(history: List<ChargeRecord>, rate: Double, amps: Double, onShare: () -> Unit) {
    Title("Share report", Icons.Filled.Share)
    NeonCard(listOf(Cyan, Blue)) {
        Text("INCLUDE IN PDF", color = Soft, fontSize = 12.sp)
        listOf("Summary page", "Full charge log", "Estimated energy and cost", "Method and assumptions").forEach {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckBox, null, tint = Blue)
                Text(it, color = White, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    NeonCard(listOf(Purple, Cyan)) {
        Text("PREVIEW", color = Soft, fontSize = 12.sp)
        SummaryRow("Charges", history.size.toString())
        SummaryRow("Estimated energy", f2(history.sumOf { it.estimatedKwh }) + " kWh")
        SummaryRow("Estimated spend", "R" + f2(history.sumOf { it.estimatedCost }))
        SummaryRow("Current tariff", "R" + f2(rate) + "/kWh")
        SummaryRow("Current setting", f1(amps) + " A")
    }
    Button(onClick = onShare, modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = White)) {
        Icon(Icons.Filled.PictureAsPdf, null)
        Spacer(Modifier.width(8.dp))
        Text("CREATE PDF & SEND VIA WHATSAPP", fontWeight = FontWeight.ExtraBold)
    }
    Text("PDF is built on this phone. No cloud or server is used.", color = Soft, fontSize = 12.sp)
}

@Composable
private fun Title(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Cyan)
        Text(text, color = White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Filters(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Box(
                Modifier.clip(RoundedCornerShape(18.dp)).clickable { onSelect(item) }
                    .background(if (item == selected) Blue else Color.Transparent)
                    .border(1.dp, if (item == selected) Blue else Soft.copy(.35f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 15.dp, vertical = 8.dp)
            ) { Text(item, color = if (item == selected) White else Soft) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = White, modifier = Modifier.weight(1f))
        Text(value, color = White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
    }
}

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)
private fun f3(v: Double) = String.format(Locale.US, "%.3f", v)

@Composable
private fun BottomNavigation(selected: Tab, onTab: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF030B16)).navigationBarsPadding().padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Tab.entries.forEach { tab ->
            val active = selected == tab
            val icon = when (tab) {
                Tab.Home -> Icons.Filled.Home
                Tab.Control -> Icons.Filled.Tune
                Tab.History -> Icons.Filled.History
                Tab.Graphs -> Icons.Filled.BarChart
                Tab.Settings -> Icons.Filled.Settings
                Tab.Share -> Icons.Filled.Share
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                    .background(if (active) Purple.copy(.65f) else Color.Transparent)
                    .clickable { onTab(tab) }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, tab.label, tint = if (active) White else Soft, modifier = Modifier.size(18.dp))
                Text(tab.label, color = if (active) White else Soft, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}
