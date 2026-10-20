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

internal enum class Tab(val label: String) {
    Home("Home"), Control("Control"), Settings("Settings"), About("About")
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

    NeonAppShell(
        tab = tab,
        onTab = { tab = it },
        status = status,
        session = session,
        now = now,
        busy = busy,
        baseUrl = baseUrl,
        token = token,
        onRefresh = ::refresh,
        onRelay1 = { on -> runAction { setRelay(1, on) } },
        onRelay2 = { on -> runAction { setRelay(2, on) } },
        onStart = { runAction { startCharging() } },
        onStop = { runAction { stopCharging() } },
        onSave = { u, t ->
            baseUrl = u.trim()
            token = t
            prefs.edit().putString("baseUrl", baseUrl).putString("token", token).apply()
            refresh()
        }
    )
}

internal fun timeText(epoch: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))

internal fun elapsedText(session: SessionState, now: Long): String {
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
