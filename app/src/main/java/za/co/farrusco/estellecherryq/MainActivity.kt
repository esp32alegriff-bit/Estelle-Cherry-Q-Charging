package za.co.farrusco.estellecherryq

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val CherryPink = Color(0xFFFF91B2)
private val CherryPinkDark = Color(0xFF3A202A)
private val AppBackground = Color(0xFF0B0F12)
private val Panel = Color(0xFF151A1E)
private val Green = Color(0xFF25F47B)
private val Red = Color(0xFFFF3045)
private val Muted = Color(0xFFAAB1B7)

private enum class Tab(val label: String, val glyph: String) {
    Home("Home", "⌂"),
    Control("Control", "⏻"),
    Settings("Settings", "⚙"),
    About("About", "ⓘ")
}

data class RelayStatus(
    val connected: Boolean = false,
    val relay1On: Boolean = false,
    val relay2On: Boolean = false,
    val message: String = "Connecting…"
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
    var baseUrl by remember {
        mutableStateOf(prefs.getString("baseUrl", "http://farrusco-cherryq-relay.local") ?: "")
    }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var tab by remember { mutableStateOf(Tab.Home) }
    var status by remember { mutableStateOf(RelayStatus()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun api() = RelayApi(baseUrl.trimEnd('/'), token)

    fun refresh() {
        scope.launch {
            status = withContext(Dispatchers.IO) { api().status() }
        }
    }

    fun runAction(action: suspend RelayApi.() -> RelayStatus) {
        if (busy) return
        scope.launch {
            busy = true
            status = withContext(Dispatchers.IO) { api().action() }
            busy = false
        }
    }

    LaunchedEffect(baseUrl) {
        while (true) {
            status = withContext(Dispatchers.IO) { api().status() }
            delay(3000)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CherryPink,
            secondary = Green,
            background = AppBackground,
            surface = Panel
        )
    ) {
        Scaffold(
            containerColor = AppBackground,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101417)) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(item.glyph, fontSize = 22.sp) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp)
            ) {
                Header()
                when (tab) {
                    Tab.Home -> HomeScreen(status, onRefresh = ::refresh)
                    Tab.Control -> ControlScreen(
                        status = status,
                        busy = busy,
                        onRelay1 = { on -> runAction { setRelay(1, on) } },
                        onRelay2 = { on -> runAction { setRelay(2, on) } },
                        onStart = { runAction { startCharging() } },
                        onStop = { runAction { stopCharging() } }
                    )
                    Tab.Settings -> SettingsScreen(
                        baseUrl = baseUrl,
                        token = token,
                        onSave = { newUrl, newToken ->
                            baseUrl = newUrl.trim()
                            token = newToken
                            prefs.edit().putString("baseUrl", baseUrl).putString("token", token).apply()
                            refresh()
                        },
                        status = status,
                        onTest = ::refresh
                    )
                    Tab.About -> AboutScreen(baseUrl)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(54.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            Text("✿", color = CherryPink, fontSize = 44.sp)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text("Estelle Cherry Q", color = CherryPink, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Text("Charging app", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
private fun HomeScreen(status: RelayStatus, onRefresh: () -> Unit) {
    RelayCard("Eskom Supply", "Relay 1", status.relay1On, "SUPPLY")
    Spacer(Modifier.height(14.dp))
    RelayCard("EV Charger", "Relay 2", status.relay2On, "CHARGER")
    Spacer(Modifier.height(14.dp))
    ConnectionCard(status, onRefresh)
}

@Composable
private fun RelayCard(title: String, subtitle: String, isOn: Boolean, noun: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = if (isOn) Green else Red, fontSize = 30.sp)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted)
                Spacer(Modifier.height(8.dp))
                Text(
                    noun + " " + if (isOn) "ON" else "OFF",
                    color = if (isOn) Green else Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Text(if (isOn) "ON" else "OFF", color = if (isOn) Green else Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConnectionCard(status: RelayStatus, onRefresh: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("System Status", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (status.connected) "● Connected" else "● Not connected",
                color = if (status.connected) Green else Red,
                fontWeight = FontWeight.Bold
            )
            Text(status.message, color = Muted, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = CherryPink, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh status", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ControlScreen(
    status: RelayStatus,
    busy: Boolean,
    onRelay1: (Boolean) -> Unit,
    onRelay2: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    ControlCard("Eskom Supply", "Relay 1", status.relay1On, busy) { onRelay1(!status.relay1On) }
    Spacer(Modifier.height(14.dp))
    ControlCard("EV Charger", "Relay 2", status.relay2On, busy) { onRelay2(!status.relay2On) }
    Spacer(Modifier.height(22.dp))

    val charging = status.relay1On && status.relay2On
    Button(
        onClick = if (charging) onStop else onStart,
        enabled = status.connected && !busy,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (charging) Red else Green,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(66.dp)
    ) {
        Text(
            if (charging) "STOP CHARGING" else "START CHARGING",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
    Text(
        "Home Wi-Fi only • Farrusco 2.4 GHz or Farrusco_5G",
        color = Muted,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
}

@Composable
private fun ControlCard(title: String, subtitle: String, isOn: Boolean, busy: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitle, color = Muted)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onClick,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOn) Green else Red,
                    contentColor = Color.Black
                ),
                modifier = Modifier.size(112.dp),
                shape = CircleShape
            ) {
                Text(if (isOn) "ON" else "OFF", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (isOn) "Currently ON • tap to turn OFF" else "Currently OFF • tap to turn ON",
                color = if (isOn) Green else Red
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    baseUrl: String,
    token: String,
    onSave: (String, String) -> Unit,
    status: RelayStatus,
    onTest: () -> Unit
) {
    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    var tokenText by remember(token) { mutableStateOf(token) }

    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Device Connection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Relay address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = tokenText,
                onValueChange = { tokenText = it },
                label = { Text("Local control token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onSave(urlText, tokenText) },
                colors = ButtonDefaults.buttonColors(containerColor = CherryPink, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onTest, modifier = Modifier.fillMaxWidth()) { Text("Test connection") }
            Spacer(Modifier.height(12.dp))
            Text(
                if (status.connected) "Connected" else "Not connected",
                color = if (status.connected) Green else Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Card(colors = CardDefaults.cardColors(containerColor = CherryPinkDark), shape = RoundedCornerShape(18.dp)) {
        Text(
            "Home network only. Works from Farrusco 2.4 GHz or Farrusco_5G when both are on the same LAN.",
            color = Color.White,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun AboutScreen(baseUrl: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✿", color = CherryPink, fontSize = 68.sp)
            Text("Estelle Cherry Q", color = CherryPink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Charging app", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(22.dp))
            InfoRow("App version", "1.0.0")
            InfoRow("Relay device", "farrusco-cherryq-relay")
            InfoRow("Address", baseUrl.removePrefix("http://").removePrefix("https://"))
            InfoRow("Designed for", "Home use only")
            Spacer(Modifier.height(18.dp))
            Text("FARRUSCO", color = Muted, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
    }
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
            message = json.optString("message", "farrusco-cherryq-relay.local")
        )
    }

    private fun request(path: String, method: String): String {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "Relay address must start with http:// or https://"
        }

        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            connection.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) connection.setRequestProperty("X-Farrusco-Token", token)
            if (method == "POST") {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(0)
                connection.outputStream.use { }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException("Relay returned HTTP " + code)
            }
            text
        } finally {
            connection.disconnect()
        }
    }
}
