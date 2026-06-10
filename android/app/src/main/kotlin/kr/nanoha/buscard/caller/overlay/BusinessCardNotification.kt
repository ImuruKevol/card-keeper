package kr.nanoha.buscard.caller.overlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kr.nanoha.buscard.caller.BuildConfig
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.ContactHistoryEntry
import kr.nanoha.buscard.caller.data.ContactHistorySummary

object BusinessCardNotification {
    fun show(context: Context, card: CachedBusinessCard, history: List<ContactHistoryEntry> = emptyList()) {
        val now = System.currentTimeMillis()
        val fallbackSummary = ContactHistorySummary(
            entries = history,
            last30DaysCount = history.count { now - it.timestamp <= THIRTY_DAYS_MS },
        )
        show(context, card, fallbackSummary)
    }

    fun show(context: Context, card: CachedBusinessCard, history: ContactHistorySummary) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "명함 수신 알림", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.WEB_BASE_URL.trimEnd('/')}/cards"))
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = if (card.isMissingCard) "등록된 명함 없음" else card.name.ifBlank { card.displayPhone.ifBlank { "명함장" } }
        val cardText = listOf(card.primaryLine, card.displayPhone).filter { it.isNotBlank() }.joinToString(" · ")
        val latestText = history.entries.firstOrNull()?.displayText ?: cardText
        val collapsedText = listOf(history.monthCountText, latestText).filter { it.isNotBlank() }.joinToString(" · ")
        val expandedText = expandedText(history, cardText)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setSubText(history.monthCountText)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        if (card.isMissingCard) {
            builder.setStyle(Notification.BigTextStyle().bigText(expandedText))
        } else {
            val bitmap = card.imagePath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
            if (bitmap == null) {
                builder.setStyle(Notification.BigTextStyle().bigText(expandedText))
            } else {
                val style = Notification.BigPictureStyle().bigPicture(bitmap)
                style.setSummaryText(expandedText)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    style.showBigPictureWhenCollapsed(true)
                }
                builder
                    .setLargeIcon(bitmap)
                    .setStyle(style)
            }
        }
        manager.notify(card.id.hashCode(), builder.build())
    }

    private fun expandedText(history: ContactHistorySummary, cardText: String): String {
        val lines = mutableListOf(history.monthCountText)
        if (cardText.isNotBlank()) lines.add(cardText)
        if (history.entries.isNotEmpty()) {
            lines.add("최근 기록")
            lines.addAll(history.entries.take(5).map { it.displayText })
        }
        return lines.joinToString("\n")
    }

    private const val CHANNEL_ID = "business-card-caller"
    private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
}
