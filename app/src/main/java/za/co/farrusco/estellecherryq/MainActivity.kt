package za.co.farrusco.estellecherryq

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class Tab(val label: String) {
    Home("Home"), Control("Control"), History("History"), Graphs("Graphs"), Settings("Settings"), Share("Share")
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
    val running: Boolean = false,
    val amps: Double = 13.0,
    val ratePerKwh: Double = 3.88
)

data class ChargeRecord(
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val amps: Double,
    val ratePerKwh: Double,
    val estimatedKwh: Double,
    val estimatedCost: Double
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
    var baseUrl by remember { mutableStateOf(prefs.getString("baseUrl", "http://192.168.1.127") ?: "http://192.168.1.127") }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var ratePerKwh by remember { mutableDoubleStateOf(prefs.getFloat("ratePerKwh", 3.88f).toDouble()) }
    var chargeAmps by remember { mutableDoubleStateOf(prefs.getFloat("chargeAmps", 13f).toDouble()) }
    var tab by remember { mutableStateOf(Tab.Home) }
    var status by remember { mutableStateOf(RelayStatus()) }
    var busy by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var history by remember { mutableStateOf(loadHistory(prefs.getString("chargeHistory", "[]") ?: "[]")) }
    var session by remember {
        mutableStateOf(
            SessionState(
                startedAt = prefs.getLong("sessionStartedAt", 0L),
                endedAt = prefs.getLong("sessionEndedAt", 0L),
                running = prefs.getBoolean("sessionRunning", false),
                amps = prefs.getFloat("sessionAmps", chargeAmps.toFloat()).toDouble(),
                ratePerKwh = prefs.getFloat("sessionRate", ratePerKwh.toFloat()).toDouble()
            )
        )
    }
    var previousRelay2 by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    fun persistHistory(records: List<ChargeRecord>) {
        history = records
        prefs.edit().putString("chargeHistory", historyToJson(records)).apply()
    }

    fun saveSession(s: SessionState) {
        session = s
        prefs.edit()
            .putLong("sessionStartedAt", s.startedAt)
            .putLong("sessionEndedAt", s.endedAt)
            .putBoolean("sessionRunning", s.running)
            .putFloat("sessionAmps", s.amps.toFloat())
            .putFloat("sessionRate", s.ratePerKwh.toFloat())
            .apply()
    }

    fun finishSession(endTime: Long) {
        if (!session.running || session.startedAt <= 0L) return
        val duration = (endTime - session.startedAt).coerceAtLeast(0L)
        val kwh = estimateKwh(duration, session.amps)
        val record = ChargeRecord(
            startedAt = session.startedAt,
            endedAt = endTime,
            durationMs = duration,
            amps = session.amps,
            ratePerKwh = session.ratePerKwh,
            estimatedKwh = kwh,
            estimatedCost = kwh * session.ratePerKwh
        )
        persistHistory((history + record).takeLast(1000))
        saveSession(session.copy(endedAt = endTime, running = false))
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
                saveSession(SessionState(System.currentTimeMillis(), 0L, true, chargeAmps, ratePerKwh))
            } else if (!status.relay2On && session.running) {
                finishSession(System.currentTimeMillis())
            }
            return@LaunchedEffect
        }
        if (!previous && status.relay2On) {
            saveSession(SessionState(System.currentTimeMillis(), 0L, true, chargeAmps, ratePerKwh))
        } else if (previous && !status.relay2On && session.running) {
            finishSession(System.currentTimeMillis())
        }
        previousRelay2 = status.relay2On
    }

    NeonAppShell(
        context = context,
        tab = tab,
        onTab = { tab = it },
        status = status,
        session = session,
        history = history,
        now = now,
        busy = busy,
        baseUrl = baseUrl,
        token = token,
        ratePerKwh = ratePerKwh,
        chargeAmps = chargeAmps,
        onRefresh = ::refresh,
        onRelay1 = { on -> runAction { setRelay(1, on) } },
        onRelay2 = { on -> runAction { setRelay(2, on) } },
        onStart = { runAction { startCharging() } },
        onStop = { runAction { stopCharging() } },
        onSave = { u, t, rate, amps ->
            baseUrl = u.trim()
            token = t
            ratePerKwh = rate.coerceAtLeast(0.0)
            chargeAmps = amps.coerceAtLeast(0.0)
            prefs.edit()
                .putString("baseUrl", baseUrl)
                .putString("token", token)
                .putFloat("ratePerKwh", ratePerKwh.toFloat())
                .putFloat("chargeAmps", chargeAmps.toFloat())
                .apply()
            refresh()
        },
        onShare = { shareHistoryPdf(context, history, ratePerKwh, chargeAmps) }
    )
}

internal fun timeText(epoch: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))
internal fun dateTimeText(epoch: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epoch))

internal fun elapsedText(session: SessionState, now: Long): String {
    if (session.startedAt <= 0L) return "00:00:00"
    val end = if (session.running) now else if (session.endedAt > 0L) session.endedAt else now
    return durationText((end - session.startedAt).coerceAtLeast(0L))
}

internal fun durationText(durationMs: Long): String {
    val total = durationMs.coerceAtLeast(0L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

internal fun estimateKwh(durationMs: Long, amps: Double): Double {
    val hours = durationMs.coerceAtLeast(0L) / 3_600_000.0
    return (230.0 * amps / 1000.0) * hours
}

private fun historyToJson(records: List<ChargeRecord>): String {
    val array = JSONArray()
    records.forEach { r ->
        array.put(JSONObject().apply {
            put("startedAt", r.startedAt)
            put("endedAt", r.endedAt)
            put("durationMs", r.durationMs)
            put("amps", r.amps)
            put("ratePerKwh", r.ratePerKwh)
            put("estimatedKwh", r.estimatedKwh)
            put("estimatedCost", r.estimatedCost)
        })
    }
    return array.toString()
}

private fun loadHistory(raw: String): List<ChargeRecord> = try {
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                ChargeRecord(
                    startedAt = o.optLong("startedAt"),
                    endedAt = o.optLong("endedAt"),
                    durationMs = o.optLong("durationMs"),
                    amps = o.optDouble("amps", 13.0),
                    ratePerKwh = o.optDouble("ratePerKwh", 3.88),
                    estimatedKwh = o.optDouble("estimatedKwh", 0.0),
                    estimatedCost = o.optDouble("estimatedCost", 0.0)
                )
            )
        }
    }
} catch (_: Exception) {
    emptyList()
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
