package kr.nanoha.buscard.caller.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kr.nanoha.buscard.caller.data.CachedBusinessCard

object CardImageRenderer {
    fun render(card: CachedBusinessCard): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(247, 251, 250))

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(34f, 34f, WIDTH - 34f, HEIGHT - 34f), 34f, 34f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(216, 230, 225)
        canvas.drawRoundRect(RectF(34f, 34f, WIDTH - 34f, HEIGHT - 34f), 34f, 34f, paint)

        paint.style = Paint.Style.FILL
        paint.color = MAIN
        canvas.drawRect(34f, 34f, 134f, HEIGHT - 34f, paint)
        paint.color = ACCENT
        canvas.drawRect(134f, 34f, 148f, HEIGHT - 34f, paint)
        canvas.drawCircle(WIDTH - 160f, 138f, 70f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = MAIN
        canvas.drawCircle(WIDTH - 120f, 186f, 70f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 72f
        paint.color = Color.rgb(23, 32, 29)
        canvas.drawText(fit(card.name.ifBlank { "이름 없음" }, paint, 720f), 210f, 190f, paint)

        paint.textSize = 36f
        paint.color = MAIN
        canvas.drawText(fit(card.company, paint, 640f), 214f, 270f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 30f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText(fit(card.primaryLine, paint, 640f), 214f, 326f, paint)

        var y = 414f
        listOf("M" to card.mobile, "T" to card.phone, "E" to card.email, "W" to card.website, "A" to card.address)
            .filter { it.second.isNotBlank() }
            .take(5)
            .forEach { (label, value) ->
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(232, 245, 242)
                canvas.drawRoundRect(RectF(214f, y - 30f, 254f, y + 10f), 10f, 10f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 22f
                paint.color = MAIN
                canvas.drawText(label, 226f, y, paint)
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 26f
                paint.color = Color.rgb(38, 49, 45)
                canvas.drawText(fit(value, paint, 760f), 278f, y, paint)
                y += 48f
            }
        return bitmap
    }

    private fun fit(value: String, paint: Paint, maxWidth: Float): String {
        val text = value.trim()
        if (text.isBlank() || paint.measureText(text) <= maxWidth) return text
        var candidate = text
        while (candidate.length > 1 && paint.measureText("$candidate...") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return "$candidate..."
    }

    private const val WIDTH = 1200
    private const val HEIGHT = 680
    private val MAIN = Color.rgb(18, 60, 105)
    private val ACCENT = Color.rgb(20, 184, 166)
}
