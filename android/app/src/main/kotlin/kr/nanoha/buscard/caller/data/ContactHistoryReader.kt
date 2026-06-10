package kr.nanoha.buscard.caller.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ContactHistoryReader {
    fun summary(context: Context, aliases: Set<String>, limit: Int = 5): ContactHistorySummary {
        if (aliases.isEmpty()) return ContactHistorySummary()
        val normalizedAliases = aliases.flatMap { PhoneNumberNormalizer.aliases(it) }.toSet()
        val calls = recentCalls(context, normalizedAliases, limit)
        val messages = recentMessages(context, normalizedAliases, limit)
        val entries = (calls.entries + messages.entries).sortedByDescending { it.timestamp }.take(limit)
        return ContactHistorySummary(
            entries = entries,
            last30DaysCount = calls.last30DaysCount + messages.last30DaysCount,
        )
    }

    fun recent(context: Context, aliases: Set<String>, limit: Int = 5): List<ContactHistoryEntry> {
        return summary(context, aliases, limit).entries
    }

    private fun recentCalls(context: Context, aliases: Set<String>, limit: Int): HistoryQueryResult {
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return HistoryQueryResult()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )
        val cursor = queryRecent(context, CallLog.Calls.CONTENT_URI, projection, CallLog.Calls.DATE) ?: return HistoryQueryResult()
        cursor.use {
            val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val rows = mutableListOf<ContactHistoryEntry>()
            val now = System.currentTimeMillis()
            val recentCutoff = now - THIRTY_DAYS_MS
            var last30DaysCount = 0
            var scanned = 0
            while (it.moveToNext() && scanned < HISTORY_SCAN_LIMIT) {
                scanned += 1
                val timestamp = it.getLong(dateIndex)
                if (timestamp < recentCutoff && rows.size >= limit) break
                val number = it.getString(numberIndex).orEmpty()
                if (!matches(number, aliases)) continue
                if (timestamp >= recentCutoff) last30DaysCount += 1
                if (rows.size >= limit) continue
                val duration = it.getLong(durationIndex)
                val durationText = formatDuration(duration)
                rows.add(
                    ContactHistoryEntry(
                        label = callTypeLabel(it.getInt(typeIndex)),
                        timeText = formatTime(timestamp, now),
                        relativeText = formatRelativeTime(timestamp, now),
                        detail = durationText,
                        timestamp = timestamp,
                    ),
                )
            }
            return HistoryQueryResult(rows, last30DaysCount)
        }
    }

    private fun recentMessages(context: Context, aliases: Set<String>, limit: Int): HistoryQueryResult {
        if (!hasPermission(context, Manifest.permission.READ_SMS)) return HistoryQueryResult()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.TYPE,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
        )
        val cursor = queryRecent(context, Telephony.Sms.CONTENT_URI, projection, Telephony.Sms.DATE) ?: return HistoryQueryResult()
        cursor.use {
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val rows = mutableListOf<ContactHistoryEntry>()
            val now = System.currentTimeMillis()
            val recentCutoff = now - THIRTY_DAYS_MS
            var last30DaysCount = 0
            var scanned = 0
            while (it.moveToNext() && scanned < HISTORY_SCAN_LIMIT) {
                scanned += 1
                val timestamp = it.getLong(dateIndex)
                if (timestamp < recentCutoff && rows.size >= limit) break
                val address = it.getString(addressIndex).orEmpty()
                if (!matches(address, aliases)) continue
                if (timestamp >= recentCutoff) last30DaysCount += 1
                if (rows.size >= limit) continue
                val body = it.getString(bodyIndex).orEmpty().replace(Regex("\\s+"), " ").take(42)
                rows.add(
                    ContactHistoryEntry(
                        label = smsTypeLabel(it.getInt(typeIndex)),
                        timeText = formatTime(timestamp, now),
                        relativeText = formatRelativeTime(timestamp, now),
                        detail = body,
                        timestamp = timestamp,
                    ),
                )
            }
            return HistoryQueryResult(rows, last30DaysCount)
        }
    }

    private fun queryRecent(context: Context, uri: Uri, projection: Array<String>, dateColumn: String): Cursor? {
        val sortWithLimit = "$dateColumn DESC LIMIT $HISTORY_SCAN_LIMIT"
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, sortWithLimit)
        }.getOrNull() ?: runCatching {
            context.contentResolver.query(uri, projection, null, null, "$dateColumn DESC")
        }.getOrNull()
    }

    private fun matches(number: String, aliases: Set<String>): Boolean {
        return PhoneNumberNormalizer.aliases(number).any { it in aliases }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun callTypeLabel(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "수신 통화"
            CallLog.Calls.OUTGOING_TYPE -> "발신 통화"
            CallLog.Calls.MISSED_TYPE -> "부재중"
            CallLog.Calls.REJECTED_TYPE -> "거절"
            CallLog.Calls.BLOCKED_TYPE -> "차단"
            else -> "통화"
        }
    }

    private fun smsTypeLabel(type: Int): String {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> "받은 문자"
            Telephony.Sms.MESSAGE_TYPE_SENT -> "보낸 문자"
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "문자 초안"
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "보낼 문자"
            else -> "문자"
        }
    }

    private fun formatTime(timestamp: Long, now: Long): String {
        val pattern = if (isToday(timestamp, now)) "HH:mm" else "MM/dd HH:mm"
        val prefix = if (isToday(timestamp, now)) "오늘 " else ""
        return prefix + SimpleDateFormat(pattern, Locale.KOREA).format(Date(timestamp))
    }

    private fun formatRelativeTime(timestamp: Long, now: Long): String {
        val diff = (now - timestamp).coerceAtLeast(0L)
        return when {
            diff < MINUTE_MS -> "방금 전"
            diff < HOUR_MS -> "${(diff / MINUTE_MS).coerceAtLeast(1L)}분 전"
            diff < ONE_DAY_MS -> "${diff / HOUR_MS}시간 전"
            else -> "${diff / ONE_DAY_MS}일 전"
        }
    }

    private fun isToday(timestamp: Long, now: Long): Boolean {
        val target = Calendar.getInstance(Locale.KOREA).apply { timeInMillis = timestamp }
        val today = Calendar.getInstance(Locale.KOREA).apply { timeInMillis = now }
        return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDuration(durationSeconds: Long): String {
        if (durationSeconds <= 0) return ""
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            minutes > 0 && seconds > 0 -> "${minutes}분 ${seconds}초"
            minutes > 0 -> "${minutes}분"
            else -> "${seconds}초"
        }
    }

    private data class HistoryQueryResult(
        val entries: List<ContactHistoryEntry> = emptyList(),
        val last30DaysCount: Int = 0,
    )

    private const val HISTORY_SCAN_LIMIT = 500
    private const val MINUTE_MS = 60L * 1000L
    private const val HOUR_MS = 60L * MINUTE_MS
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val THIRTY_DAYS_MS = 30L * ONE_DAY_MS
}

data class ContactHistorySummary(
    val entries: List<ContactHistoryEntry> = emptyList(),
    val last30DaysCount: Int = 0,
) {
    val monthCountText: String
        get() = if (last30DaysCount > 0) "최근 한 달 기록 ${last30DaysCount}건" else "최근 한 달 기록 없음"
}

data class ContactHistoryEntry(
    val label: String,
    val timeText: String,
    val relativeText: String,
    val detail: String,
    val timestamp: Long,
) {
    val displayText: String
        get() = listOf(label, timeText, relativeText, detail).filter { it.isNotBlank() }.joinToString(" · ")
}
