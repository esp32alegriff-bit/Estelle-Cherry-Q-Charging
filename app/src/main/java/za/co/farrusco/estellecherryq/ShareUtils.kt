package za.co.farrusco.estellecherryq

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun shareHistoryPdf(
    context: Context,
    history: List<ChargeRecord>,
    currentRate: Double,
    currentAmps: Double
) {
    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(reportsDir, "Estelle-Cherry-Q-Charging-" + System.currentTimeMillis() + ".pdf")
    val pdf = PdfDocument()
    val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
    val headingPaint = Paint().apply { textSize = 15f; isFakeBoldText = true }
    val bodyPaint = Paint().apply { textSize = 11f }
    val smallPaint = Paint().apply { textSize = 9f }
    val pageWidth = 595
    val pageHeight = 842
    val margin = 42f
    var pageNumber = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = 56f

    fun newPage() {
        pdf.finishPage(page)
        pageNumber += 1
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = 52f
    }

    fun line(text: String, paint: Paint = bodyPaint, gap: Float = 18f) {
        if (y > pageHeight - 50f) newPage()
        canvas.drawText(text, margin, y, paint)
        y += gap
    }

    val totalKwh = history.sumOf { it.estimatedKwh }
    val totalCost = history.sumOf { it.estimatedCost }
    val totalDuration = history.sumOf { it.durationMs }

    line("Estelle Cherry Q Charging", titlePaint, 30f)
    line("Charging history report", headingPaint, 24f)
    line("Created: " + SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()))
    line("Sessions: " + history.size)
    line("Estimated energy: " + String.format(Locale.US, "%.2f", totalKwh) + " kWh")
    line("Estimated spend: R" + String.format(Locale.US, "%.2f", totalCost))
    line("Total charging time: " + durationText(totalDuration))
    line("Current settings: " + String.format(Locale.US, "%.1f", currentAmps) + " A, R" + String.format(Locale.US, "%.2f", currentRate) + "/kWh")
    y += 8f
    line("Session history", headingPaint, 24f)

    if (history.isEmpty()) {
        line("No completed charging sessions recorded on this phone.")
    } else {
        history.asReversed().forEachIndexed { index, r ->
            line((index + 1).toString() + ". " + dateTimeText(r.startedAt), headingPaint, 17f)
            line("   End: " + dateTimeText(r.endedAt) + "   Duration: " + durationText(r.durationMs), bodyPaint, 15f)
            line("   " + String.format(Locale.US, "%.1f", r.amps) + " A   Estimated " +
                String.format(Locale.US, "%.3f", r.estimatedKwh) + " kWh   R" +
                String.format(Locale.US, "%.2f", r.estimatedCost), bodyPaint, 16f)
        }
    }

    y += 10f
    line("Method: estimated energy = 230 V x configured current x elapsed time.", smallPaint, 13f)
    line("Values are estimates until measured EV energy data is integrated.", smallPaint, 13f)

    pdf.finishPage(page)
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()

    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(send)
    } catch (_: Exception) {
        send.setPackage(null)
        context.startActivity(Intent.createChooser(send, "Share charging report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
