package za.co.farrusco.estellecherryq

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF020914)
private val Panel = Color(0xFF071425)
private val Cyan = Color(0xFF19D7FF)
private val Blue = Color(0xFF277BFF)
private val Purple = Color(0xFFB22BFF)
private val Pink = Color(0xFFFF73D1)
private val Green = Color(0xFF19F66B)
private val Red = Color(0xFFFF334A)
private val TextSoft = Color(0xFFB6C9E8)

private enum class Tab(val label: String, val glyph: String) {
    Home("Home", "⌂"), Control("Control", "☷"), Settings("Settings", "⚙"), About("About", "ⓘ")
}

data class RelayStatus(
    val connected: Boolean = false,
    val relay1On: Boolean = false,
    val relay2On: Boolean = false,
    val message: String = "Connecting…"
)

data class SessionState(
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val running: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EstelleCherryQApp(applicationContext) }
    }
}

@Composable
private fun EstelleCherryQApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("estelle_cherry_q", Context.MODE_PRIVATE) }
    var baseUrl by remember { mutableStateOf(prefs.getString("baseUrl", "http://farrusco-cherryq-relay.local") ?: "") }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var tab by remember { mutableStateOf(Tab.Home) }
    var status by remember { mutableStateOf(RelayStatus()) }
    var busy by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var session by remember {
        mutableStateOf(
            SessionState(
                startedAt = prefs.getLong("sessionStartedAt", 0L),
                endedAt = prefs.getLong("sessionEndedAt", 0L),
                running = prefs.getBoolean("sessionRunning", false)
            )
        )
    }
    var previousRelay2 by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    fun saveSession(s: SessionState) {
        session = s
        prefs.edit()
            .putLong("sessionStartedAt", s.startedAt)
            .putLong("sessionEndedAt", s.endedAt)
            .putBoolean("sessionRunning", s.running)
            .apply()
    }

    fun api() = RelayApi(baseUrl.trimEnd('/'), token)

    fun refresh() {
        scope.launch { status = withContext(Dispatchers.IO) { api().status() } }
    }

    fun runAction(action: suspend RelayApi.() -> RelayStatus) {
        if (busy) return
        scope.launch {
            busy = true
            status = withContext(Dispatchers.IO) { api().action() }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    LaunchedEffect(baseUrl) {
        while (true) {
            status = withContext(Dispatchers.IO) { api().status() }
            delay(3000)
        }
    }

    LaunchedEffect(status.connected, status.relay2On) {
        if (!status.connected) return@LaunchedEffect
        val previous = previousRelay2
        if (previous == null) {
            previousRelay2 = status.relay2On
            if (status.relay2On && !session.running) {
                saveSession(SessionState(System.currentTimeMillis(), 0L, true))
            }
            return@LaunchedEffect
        }
        if (!previous && status.relay2On) {
            saveSession(SessionState(System.currentTimeMillis(), 0L, true))
        } else if (previous && !status.relay2On && session.running) {
            saveSession(session.copy(endedAt = System.currentTimeMillis(), running = false))
        }
        previousRelay2 = status.relay2On
    }

    MaterialTheme(
        colorScheme = darkColorScheme(primary = Purple, secondary = Cyan, background = Bg, surface = Panel)
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF030B16)) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF6E1EC8),
                                unselectedIconColor = TextSoft,
                                unselectedTextColor = TextSoft
                            ),
                            icon = { Text(item.glyph, fontSize = 23.sp) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(Bg)
                    .padding(horizontal = 14.dp)
            ) {
                Header()
                when (tab) {
                    Tab.Home -> HomeScreen(status, session, now, ::refresh)
                    Tab.Control -> ControlScreen(
                        status, session, now, busy,
                        onRelay1 = { on -> runAction { setRelay(1, on) } },
                        onRelay2 = { on -> runAction { setRelay(2, on) } },
                        onStart = { runAction { startCharging() } },
                        onStop = { runAction { stopCharging() } }
                    )
                    Tab.Settings -> SettingsScreen(
                        baseUrl, token, status, session, now,
                        onSave = { u, t ->
                            baseUrl = u.trim()
                            token = t
                            prefs.edit().putString("baseUrl", baseUrl).putString("token", token).apply()
                            refresh()
                        },
                        onTest = ::refresh
                    )
                    Tab.About -> AboutScreen(baseUrl, session, now)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF16023B), Color(0xFF03142E), Color(0xFF002A47))
                )
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✿", color = Color(0xFFEAC2FF), fontSize = 48.sp)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Estelle Cherry Q", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp)
                Text("Charging app", color = TextSoft, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(Purple, Cyan, Color.Transparent)))
        )
    }
}

@Composable
private fun NeonCard(
    borderColors: List<Color> = listOf(Cyan, Blue),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Brush.horizontalGradient(borderColors), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.96f))
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun HomeScreen(status: RelayStatus, session: SessionState, now: Long, onRefresh: () -> Unit) {
    NeonCard(listOf(Cyan, Green)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◉", color = if (status.connected) Green else Red, fontSize = 38.sp)
            Column(Modifier.padding(start = 14.dp)) {
                Text("System Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(if (status.connected) "● Connected" else "● Not connected",
                    color = if (status.connected) Green else Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (status.connected) "Relay online • Home Wi‑Fi" else status.message, color = TextSoft)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    DeviceSummaryCard("⚡", "Eskom Supply", "Relay 1", status.relay1On, "SUPPLY ACTIVE", listOf(Cyan, Blue))
    Spacer(Modifier.height(12.dp))
    DeviceSummaryCard("🔌", "EV Charger", "Relay 2", status.relay2On, if (status.relay2On) "CHARGER ON" else "CHARGER OFF", listOf(Purple, Blue))
    Spacer(Modifier.height(12.dp))
    SessionCard(session, now)
    if (!status.connected) {
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B8CD8))
        ) { Text("Refresh connection", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DeviceSummaryCard(icon: String, title: String, relay: String, on: Boolean, stateText: String, colors: List<Color>) {
    NeonCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, Brush.linearGradient(colors), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 34.sp) }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                Text(relay, color = TextSoft, fontSize = 16.sp)
                Text(stateText, color = if (on) Green else Red, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Surface(
                color = if (on) Green else Red,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (on) "ON" else "OFF", color = Color.Black, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun ControlScreen(
    status: RelayStatus,
    session: SessionState,
    now: Long,
    busy: Boolean,
    onRelay1: (Boolean) -> Unit,
    onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    RelayControlCard("⚡", "Eskom Supply", "Relay 1", status.relay1On, listOf(Cyan, Blue), busy) { onRelay1(!status.relay1On) }
    Spacer(Modifier.height(12.dp))
    RelayControlCard("🔌", "EV Charger", "Relay 2", status.relay2On, listOf(Purple, Blue), busy) { onRelay2(!status.relay2On) }
    Spacer(Modifier.height(12.dp))
    SessionCard(session, now)
    Spacer(Modifier.height(12.dp))
    val charging = status.relay1On && status.relay2On
    Button(
        onClick = if (charging) onStop else onStart,
        enabled = status.connected && !busy,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (charging) Red else Green, contentColor = Color.Black)
    ) {
        Text(if (charging) "STOP CHARGING" else "START CHARGING", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun RelayControlCard(icon: String, title: String, relay: String, on: Boolean, colors: List<Color>, busy: Boolean, onClick: () -> Unit) {
    NeonCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).border(2.dp, Brush.linearGradient(colors), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 34.sp) }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                Text(relay, color = TextSoft)
                Text(if (on) "● Supply Active" else "● Offline", color = if (on) Green else Red, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onClick,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(width = 190.dp, height = 72.dp),
            shape = RoundedCornerShape(36.dp),
            border = BorderStroke(2.dp, if (on) Color(0xFF67FFAA) else Color(0xFFFF7385)),
            colors = ButtonDefaults.buttonColors(containerColor = if (on) Color(0xFF00C94C) else Color(0xFFE91632), contentColor = Color.White)
        ) {
            Text("⏻  " + if (on) "ON" else "OFF", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            if (on) "Currently ON • tap to turn OFF" else "Currently OFF • tap to turn ON",
            color = if (on) Green else Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
        )
    }
}

@Composable
private fun SessionCard(session: SessionState, now: Long) {
    NeonCard(listOf(Cyan, Blue)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◷", color = Cyan, fontSize = 34.sp)
            Text("Charging Session", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
        }
        Spacer(Modifier.height(10.dp))
        SessionRow("▶", Green, "Started", if (session.startedAt > 0) timeText(session.startedAt) else "--:--")
        SessionRow("◷", Cyan, "Current time", timeText(now))
        SessionRow("⌛", Pink, "Elapsed", elapsedText(session, now))
        SessionRow("■", Color(0xFFAFC2FF), "Ended", if (!session.running && session.endedAt > 0) timeText(session.endedAt) else "--:--")
    }
}

@Composable
private fun SessionRow(icon: String, color: Color, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = color, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Text(label, color = TextSoft, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun SettingsScreen(
    baseUrl: String,
    token: String,
    status: RelayStatus,
    session: SessionState,
    now: Long,
    onSave: (String, String) -> Unit,
    onTest: () -> Unit
) {
    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    var tokenText by remember(token) { mutableStateOf(token) }

    NeonCard(listOf(Cyan, Blue)) {
        Text("🔗  Device Connection", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Configure connection to your relay device", color = TextSoft)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(urlText, { urlText = it }, label = { Text("Relay address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(tokenText, { tokenText = it }, label = { Text("Local control token") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onSave(urlText, tokenText) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)
            ) { Text("💾 Save", fontWeight = FontWeight.Bold) }
            Button(
                onClick = onTest,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A9E8))
            ) { Text("📶 Test connection", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (status.connected) "● Connected  •  Relay online • Home Wi‑Fi" else "● Not connected",
            color = if (status.connected) Green else Red,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(Modifier.height(12.dp))
    SessionCard(session, now)
    Spacer(Modifier.height(12.dp))
    NeonCard(listOf(Purple, Blue)) {
        Text("📶  Home network only.", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Works from Farrusco 2.4 GHz or Farrusco_5G", color = TextSoft)
    }
}

@Composable
private fun AboutScreen(baseUrl: String, session: SessionState, now: Long) {
    NeonCard(listOf(Cyan, Purple)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("✿", color = Color(0xFFEAC2FF), fontSize = 66.sp)
            Text("Estelle Cherry Q", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
            Text("Charging app", color = TextSoft, fontSize = 18.sp)
            Spacer(Modifier.height(20.dp))
            AboutRow("◈", Cyan, "App version", "EF_1.1")
            AboutRow("⚙", Green, "Relay device", "farrusco-cherryq-relay")
            AboutRow("🔗", Pink, "Address", baseUrl.removePrefix("http://").removePrefix("https://"))
            AboutRow("⌂", Cyan, "Designed for", "Home use only")
            Spacer(Modifier.height(14.dp))
            Text("F A R R U S C O", color = TextSoft, letterSpacing = 3.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    SessionCard(session, now)
}

@Composable
private fun AboutRow(icon: String, color: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = color, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Text(label, color = TextSoft, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
    }
}

private fun timeText(epoch: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))

private fun elapsedText(session: SessionState, now: Long): String {
    if (session.startedAt <= 0L) return "00:00:00"
    val end = if (session.running) now else if (session.endedAt > 0L) session.endedAt else now
    val total = ((end - session.startedAt).coerceAtLeast(0L) / 1000L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private class RelayApi(private val baseUrl: String, private val token: String) {
    fun status(): RelayStatus = try {
        parse(request("/api/v1/status", "GET"))
    } catch (e: Exception) {
        RelayStatus(message = e.message ?: "Relay unavailable")
    }

    fun setRelay(relay: Int, on: Boolean): RelayStatus =
        runAndRead("/api/v1/relay/" + relay + "/" + if (on) "on" else "off")

    fun startCharging(): RelayStatus = runAndRead("/api/v1/charge/start")
    fun stopCharging(): RelayStatus = runAndRead("/api/v1/charge/stop")

    private fun runAndRead(path: String): RelayStatus = try {
        val body = request(path, "POST")
        if (body.isBlank()) status() else parse(body)
    } catch (e: Exception) {
        RelayStatus(message = e.message ?: "Command failed")
    }

    private fun parse(raw: String): RelayStatus {
        val json = JSONObject(raw)
        return RelayStatus(
            connected = true,
            relay1On = json.optBoolean("relay1_on", false),
            relay2On = json.optBoolean("relay2_on", false),
            message = json.optString("message", "Connected")
        )
    }

    private fun request(path: String, method: String): String {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) { "Relay address must start with http:// or https://" }
        val c = URL(baseUrl + path).openConnection() as HttpURLConnection
        return try {
            c.requestMethod = method
            c.connectTimeout = 2500
            c.readTimeout = 2500
            c.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) c.setRequestProperty("X-Farrusco-Token", token)
            if (method == "POST") {
                c.doOutput = true
                c.setFixedLengthStreamingMode(0)
                c.outputStream.use { }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Relay returned HTTP " + code)
            text
        } finally {
            c.disconnect()
        }
    }
}
