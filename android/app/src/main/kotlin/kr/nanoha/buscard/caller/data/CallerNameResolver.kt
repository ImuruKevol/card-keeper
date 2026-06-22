package kr.nanoha.buscard.caller.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer

object CallerNameResolver {
    fun resolveName(context: Context, rawNumber: String, aliases: Set<String> = PhoneNumberNormalizer.aliases(rawNumber)): String {
        val candidates = numberCandidates(rawNumber, aliases)
        return fromContacts(context, candidates)
            ?: fromCallLogCache(context, aliases)
            ?: ""
    }

    private fun numberCandidates(rawNumber: String, aliases: Set<String>): List<String> {
        return linkedSetOf<String>().apply {
            rawNumber.trim().takeIf { it.isNotBlank() }?.let(::add)
            addAll(aliases)
            aliases.map { PhoneNumberNormalizer.display(it) }.filter { it.isNotBlank() }.forEach(::add)
        }.toList()
    }

    private fun fromContacts(context: Context, candidates: List<String>): String? {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return null
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        candidates.forEach { number ->
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val cursor = runCatching {
                context.contentResolver.query(uri, projection, null, null, null)
            }.getOrNull() ?: return@forEach

            cursor.use {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                while (it.moveToNext()) {
                    val name = cleanName(it.stringOrEmpty(nameIndex))
                    if (name.isNotBlank()) return name
                }
            }
        }

        return null
    }

    private fun fromCallLogCache(context: Context, aliases: Set<String>): String? {
        if (aliases.isEmpty() || !hasPermission(context, Manifest.permission.READ_CALL_LOG)) return null
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE)
        val cursor = queryRecentCalls(context, projection) ?: return null

        cursor.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            var scanned = 0
            while (it.moveToNext() && scanned < CALL_LOG_SCAN_LIMIT) {
                scanned += 1
                val number = it.stringOrEmpty(numberIndex)
                if (!matches(number, aliases)) continue

                val name = cleanName(it.stringOrEmpty(nameIndex))
                if (name.isNotBlank()) return name
            }
        }

        return null
    }

    private fun queryRecentCalls(context: Context, projection: Array<String>): Cursor? {
        return runCatching {
            context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT $CALL_LOG_SCAN_LIMIT")
        }.getOrNull() ?: runCatching {
            context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC")
        }.getOrNull()
    }

    private fun matches(number: String, aliases: Set<String>): Boolean {
        return PhoneNumberNormalizer.aliases(number).any { it in aliases }
    }

    private fun cleanName(value: String): String {
        val name = value.trim().replace(Regex("\\s+"), " ")
        if (name.isBlank()) return ""
        if (name.all { it.isDigit() || it in "+()- .#" }) return ""
        return name.take(MAX_NAME_LENGTH)
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun Cursor.stringOrEmpty(index: Int): String {
        return if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""
    }

    private const val CALL_LOG_SCAN_LIMIT = 200
    private const val MAX_NAME_LENGTH = 80
}
