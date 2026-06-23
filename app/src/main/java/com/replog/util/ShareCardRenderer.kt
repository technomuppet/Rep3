package com.replog.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Phase D — Shareable Cards (free marketing).
 *
 * Renders a branded RepLog card to a PNG using only android.graphics (no
 * external fonts/images/CDN), saves it to cache, and shares it via the existing
 * FileProvider. Works fully offline.
 */
data class ShareStat(val label: String, val value: String)

object ShareCardRenderer {

    private const val W = 1080
    private const val H = 1080
    private val BG_TOP = Color.parseColor("#0E1116")
    private val BG_BOTTOM = Color.parseColor("#1B2330")
    private val ACCENT = Color.parseColor("#4F8CFF")
    private val WHITE = Color.parseColor("#FFFFFF")
    private val MUTED = Color.parseColor("#9AA6B2")

    /**
     * Render a card and return the saved PNG file (in cacheDir), or null on failure.
     * @param headline big top line, e.g. "Workout Complete" or "Bench +20kg"
     * @param subtitle smaller line under the headline (e.g. workout name / date range)
     * @param stats up to 4 stat tiles
     * @param footnote bottom line (e.g. a PR call-out); optional
     */
    fun render(
        context: Context,
        headline: String,
        subtitle: String,
        stats: List<ShareStat>,
        footnote: String? = null,
        fileName: String = "replog_card_${System.currentTimeMillis()}.png"
    ): File? = runCatching {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bgPaint)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val regular = Typeface.DEFAULT

        // Brand
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT; textSize = 44f; typeface = bold }
        c.drawText("REPLOG", 80f, 130f, brand)
        val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 30f; typeface = regular }
        c.drawText("Track every rep. Beat every best.", 80f, 178f, tag)

        // Headline (wrapped)
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; textSize = 96f; typeface = bold }
        var y = 360f
        for (line in wrap(headline, headPaint, (W - 160).toFloat())) {
            c.drawText(line, 80f, y, headPaint)
            y += 110f
        }

        // Subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 40f; typeface = regular }
        if (subtitle.isNotBlank()) {
            c.drawText(ellipsize(subtitle, subPaint, (W - 160).toFloat()), 80f, y + 10f, subPaint)
            y += 70f
        }

        // Stat tiles (2x2 grid)
        val gridTop = 560f
        val tileW = (W - 160 - 40) / 2f
        val tileH = 180f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 32f; typeface = regular }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; textSize = 64f; typeface = bold }
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#22FFFFFF") }
        stats.take(4).forEachIndexed { i, stat ->
            val col = i % 2
            val row = i / 2
            val left = 80f + col * (tileW + 40)
            val top = gridTop + row * (tileH + 30)
            c.drawRoundRect(left, top, left + tileW, top + tileH, 28f, 28f, tilePaint)
            c.drawText(stat.label.uppercase(), left + 30f, top + 56f, labelPaint)
            c.drawText(ellipsize(stat.value, valuePaint, tileW - 60), left + 30f, top + 130f, valuePaint)
        }

        // Footnote
        footnote?.takeIf { it.isNotBlank() }?.let {
            val fnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT; textSize = 40f; typeface = bold }
            c.drawText(ellipsize(it, fnPaint, (W - 160).toFloat()), 80f, (H - 90).toFloat(), fnPaint)
        }

        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        file
    }.getOrNull()

    /** Render and immediately open the system share sheet. */
    fun renderAndShare(
        context: Context,
        headline: String,
        subtitle: String,
        stats: List<ShareStat>,
        footnote: String? = null
    ) {
        val file = render(context, headline, subtitle, stats, footnote) ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My RepLog progress 💪")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share progress"))
    }

    // --- text helpers ---

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }.take(3)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }
}
