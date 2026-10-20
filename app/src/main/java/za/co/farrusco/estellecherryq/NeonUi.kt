package za.co.farrusco.estellecherryq

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFF020812)
private val Navy = Color(0xFF071426)
private val Cyan = Color(0xFF19D7FF)
private val Blue = Color(0xFF28A8FF)
private val Purple = Color(0xFFA72BFF)
private val Lavender = Color(0xFFDCA6FF)
private val Pink = Color(0xFFFF73D1)
private val Green = Color(0xFF12F469)
private val Red = Color(0xFFFF334A)
private val Soft = Color(0xFF9FB7D9)
private val White = Color(0xFFF5FAFF)

@Composable
internal fun NeonAppShell(
    tab: Tab,
    onTab: (Tab) -> Unit,
    status: RelayStatus,
    session: SessionState,
    now: Long,
    busy: Boolean,
    baseUrl: String,
    token: String,
    onRefresh: () -> Unit,
    onRelay1: (Boolean) -> Unit,
    onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: (String, String) -> Unit
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
                AppHeader()
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    when (tab) {
                        Tab.Home -> HomeScreen(status, session, now, onRefresh)
                        Tab.Control -> ControlScreen(status, session, now, busy, onRelay1, onRelay2, onStart, onStop)
                        Tab.Settings -> SettingsScreen(baseUrl, token, status, session, now, onSave, onRefresh)
                        Tab.About -> AboutScreen(baseUrl, session, now)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowerLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val petalDistance = size.minDimension * .22f
        val petalRadius = size.minDimension * .18f
        repeat(6) { index ->
            val angle = 2 * PI * index / 6
            drawCircle(
                color = if (index % 2 == 0) Lavender else Pink,
                radius = petalRadius,
                center = Offset(center.x + (cos(angle) * petalDistance).toFloat(), center.y + (sin(angle) * petalDistance).toFloat())
            )
        }
        drawCircle(color = Color(0xFFFFE5FD), radius = size.minDimension * .10f, center = center)
    }
}

@Composable
private fun AppHeader() {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF260749), Color(0xFF0B1C4B), Color(0xFF073354))))
    ) {
        Row(
            Modifier.fillMaxWidth().height(86.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(50.dp).shadow(12.dp, CircleShape, ambientColor = Purple, spotColor = Purple)
                .background(Color(0xFF26194D), CircleShape), contentAlignment = Alignment.Center) {
                FlowerLogo(Modifier.size(36.dp))
            }
            Column(Modifier.padding(start = 13.dp)) {
                Text("Estelle Cherry Q", color = White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("Charging app", color = Lavender, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp)
            .background(Brush.horizontalGradient(listOf(Purple, Blue, Cyan))))
    }
}

@Composable
private fun NeonCard(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(19.dp)
    Box(modifier.fillMaxWidth().shadow(9.dp, shape, ambientColor = colors.first().copy(alpha = .28f), spotColor = colors.last().copy(alpha = .27f))
        .background(Brush.linearGradient(colors.map { it.copy(alpha = .82f) }), shape).padding(1.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0D2340), Color(0xFF071322), Color(0xFF09182D))))
                .padding(horizontal = 15.dp, vertical = 13.dp),
            content = content
        )
    }
}

@Composable
private fun IconTile(icon: ImageVector, colors: List<Color>) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(colors.map { it.copy(alpha = .22f) }))
            .border(1.dp, colors.first().copy(alpha = .55f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = colors.first(), modifier = Modifier.size(27.dp)) }
}

@Composable
private fun NeonStatusBadge(on: Boolean) {
    val color = if (on) Green else Red
    Box(Modifier.shadow(7.dp, RoundedCornerShape(13.dp), ambientColor = color, spotColor = color)
        .background(color.copy(alpha = .16f), RoundedCornerShape(13.dp))
        .border(1.dp, color.copy(alpha = .8f), RoundedCornerShape(13.dp))
        .padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(if (on) "ON" else "OFF", color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HomeScreen(status: RelayStatus, session: SessionState, now: Long, onRefresh: () -> Unit) {
    NeonCard(listOf(if (status.connected) Green else Red, Cyan)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (status.connected) Icons.Filled.NetworkCheck else Icons.Filled.Wifi,
                listOf(if (status.connected) Green else Red, Cyan))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("System Status", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (status.connected) "Connected" else "Not connected", color = if (status.connected) Green else Red,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(if (status.connected) "Relay online • Home Wi-Fi" else status.message,
                    color = Soft, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Soft, modifier = Modifier.size(18.dp))
        }
        if (!status.connected) {
            Spacer(Modifier.height(8.dp))
            Text("Retry connection", color = Cyan, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onRefresh))
        }
    }
    DeviceSummaryCard(Icons.Filled.FlashOn, "Eskom Supply", "Relay 1", status.relay1On,
        if (status.relay1On) "SUPPLY ACTIVE" else "SUPPLY OFF", listOf(Cyan, Blue))
    DeviceSummaryCard(Icons.Filled.EvStation, "EV Charger", "Relay 2", status.relay2On,
        if (status.relay2On) "CHARGER ON" else "CHARGER OFF", listOf(Purple, Blue))
    SessionPanel(session, now)
}

@Composable
private fun DeviceSummaryCard(icon: ImageVector, title: String, relay: String, on: Boolean, state: String, colors: List<Color>) {
    NeonCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, colors)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(relay, color = Soft, fontSize = 12.sp)
                Text(state, color = if (on) Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            NeonStatusBadge(on)
        }
    }
}

@Composable
private fun ControlScreen(
    status: RelayStatus, session: SessionState, now: Long, busy: Boolean,
    onRelay1: (Boolean) -> Unit, onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit, onStop: () -> Unit
) {
    RelayControlCard(Icons.Filled.FlashOn, "Eskom Supply", "Relay 1", status.relay1On,
        if (status.relay1On) "Supply Active" else "Supply Off", listOf(Cyan, Blue), status.connected && !busy) {
        onRelay1(!status.relay1On)
    }
    RelayControlCard(Icons.Filled.EvStation, "EV Charger", "Relay 2", status.relay2On,
        if (status.relay2On) "Charger On" else "Charger Off", listOf(Purple, Blue), status.connected && !busy) {
        onRelay2(!status.relay2On)
    }
    SessionPanel(session, now, compact = true)
    val charging = status.relay1On && status.relay2On
    NeonCard(listOf(if (charging) Red else Green, Blue)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Charging control", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Safe two-relay sequence", color = Soft, fontSize = 12.sp)
            }
            Button(
                onClick = if (charging) onStop else onStart,
                enabled = status.connected && !busy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (charging) Red else Green, contentColor = Night),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(if (charging) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (charging) "STOP" else "START", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RelayControlCard(
    icon: ImageVector, title: String, relay: String, on: Boolean, state: String,
    colors: List<Color>, enabled: Boolean, onClick: () -> Unit
) {
    NeonCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, colors)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("$relay  •  $state", color = if (on) Green else Soft, fontSize = 12.sp)
            }
            NeonPowerButton(on, enabled, onClick)
        }
        Text(if (on) "Currently ON • tap to turn OFF" else "Currently OFF • tap to turn ON",
            color = if (on) Green else Red, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.End).padding(top = 5.dp))
    }
}

@Composable
private fun NeonPowerButton(on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = if (on) Green else Red
    Column(
        Modifier.shadow(9.dp, RoundedCornerShape(15.dp), ambientColor = color.copy(alpha = .7f), spotColor = color.copy(alpha = .7f))
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = .42f), color.copy(alpha = .16f))))
            .border(1.dp, color, RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PowerSettingsNew, contentDescription = if (on) "Turn off" else "Turn on", tint = color, modifier = Modifier.size(21.dp))
        Text(if (on) "ON" else "OFF", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

@Composable
private fun SessionPanel(session: SessionState, now: Long, compact: Boolean = false) {
    NeonCard(listOf(Cyan, Purple)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
            Text("Charging Session", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        val started = if (session.startedAt > 0) timeText(session.startedAt) else "--:--"
        val ended = if (!session.running && session.endedAt > 0) timeText(session.endedAt) else "--:--"
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    SessionField(Icons.Filled.PlayArrow, Green, "Started", started)
                    SessionField(Icons.Filled.AccessTime, Cyan, "Current time", timeText(now))
                }
                Column(Modifier.weight(1f)) {
                    SessionField(Icons.Filled.BatteryChargingFull, Pink, "Elapsed", elapsedText(session, now))
                    SessionField(Icons.Filled.Stop, Lavender, "Ended", ended)
                }
            }
        } else {
            SessionField(Icons.Filled.PlayArrow, Green, "Started", started)
            SessionField(Icons.Filled.AccessTime, Cyan, "Current time", timeText(now))
            SessionField(Icons.Filled.BatteryChargingFull, Pink, "Elapsed", elapsedText(session, now))
            SessionField(Icons.Filled.Stop, Lavender, "Ended", ended)
        }
    }
}

@Composable
private fun SessionField(icon: ImageVector, color: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, color = Soft, fontSize = 12.sp, maxLines = 1,
            modifier = Modifier.weight(1f).padding(start = 6.dp), overflow = TextOverflow.Ellipsis)
        Text(value, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SettingsScreen(
    baseUrl: String, token: String, status: RelayStatus, session: SessionState, now: Long,
    onSave: (String, String) -> Unit, onTest: () -> Unit
) {
    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    var tokenText by remember(token) { mutableStateOf(token) }
    NeonCard(listOf(Cyan, Blue)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
            Text("Device Connection", color = White, fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Text("Connect to your home relay", color = Soft, fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(urlText, { urlText = it }, label = { Text("Relay address") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(tokenText, { tokenText = it }, label = { Text("Local control token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(urlText, tokenText) }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Night),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(onClick = onTest, modifier = Modifier.weight(1.35f),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Night),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("TEST CONNECTION", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
    NeonCard(listOf(if (status.connected) Green else Red, Cyan)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (status.connected) Icons.Filled.CheckCircle else Icons.Filled.Wifi,
                contentDescription = null, tint = if (status.connected) Green else Red, modifier = Modifier.size(24.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(if (status.connected) "Connected" else "Not connected", color = if (status.connected) Green else Red,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (status.connected) "Relay online • Home Wi-Fi" else status.message,
                    color = Soft, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    SessionPanel(session, now)
    NeonCard(listOf(Purple, Blue)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Lavender, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 9.dp)) {
                Text("Home network only", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Works from Farrusco 2.4 GHz or Farrusco_5G", color = Soft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AboutScreen(baseUrl: String, session: SessionState, now: Long) {
    NeonCard(listOf(Purple, Cyan)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            FlowerLogo(Modifier.size(56.dp))
            Text("Estelle Cherry Q", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text("Charging app", color = Lavender, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            AboutRow(Icons.Filled.Info, Cyan, "App version", "EF_1.2")
            AboutRow(Icons.Filled.Settings, Green, "Relay device", "farrusco-cherryq-relay")
            AboutRow(Icons.Filled.Link, Pink, "Address", baseUrl)
            AboutRow(Icons.Filled.Home, Cyan, "Designed for", "Home use only")
        }
    }
    SessionPanel(session, now, compact = true)
    Text("F A R R U S C O", color = Lavender, fontSize = 12.sp, letterSpacing = 3.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), textAlign = TextAlign.Center)
}

@Composable
private fun AboutRow(icon: ImageVector, color: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Text(label, color = Soft, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
        Text(value, color = White, fontSize = 12.sp, textAlign = TextAlign.End,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.35f))
    }
}

@Composable
private fun BottomNavigation(selected: Tab, onTab: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF030B16)).navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.entries.forEach { tab ->
            val active = selected == tab
            val icon = when (tab) {
                Tab.Home -> Icons.Filled.Home
                Tab.Control -> Icons.Filled.Tune
                Tab.Settings -> Icons.Filled.Settings
                Tab.About -> Icons.Filled.Info
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (active) Brush.linearGradient(listOf(Color(0xFF6F20BA), Color(0xFF4F1A91)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onTab(tab) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = tab.label, tint = if (active) White else Soft, modifier = Modifier.size(20.dp))
                Text(tab.label, color = if (active) White else Soft, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
