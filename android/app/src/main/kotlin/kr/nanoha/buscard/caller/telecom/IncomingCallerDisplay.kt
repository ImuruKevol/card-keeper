package kr.nanoha.buscard.caller.telecom

import android.content.Context
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import kr.nanoha.buscard.caller.data.BusinessCardDatabase
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.ContactHistoryReader
import kr.nanoha.buscard.caller.overlay.BusinessCardNotification

object IncomingCallerDisplay {
    private var lastKey = ""
    private var lastShownAt = 0L

    fun show(context: Context, rawNumber: String): Boolean {
        val appContext = context.applicationContext
        val aliases = PhoneNumberNormalizer.aliases(rawNumber)
        if (aliases.isEmpty()) return false

        val key = aliases.sorted().joinToString("|")
        if (isDuplicate(key)) return true

        val card = BusinessCardDatabase(appContext).findByPhoneAliases(aliases) ?: missingCard(rawNumber)
        val history = ContactHistoryReader.summary(appContext, aliases)
        BusinessCardNotification.show(appContext, card, history)
        return true
    }

    @Synchronized
    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        val duplicate = key == lastKey && now - lastShownAt < DUPLICATE_WINDOW_MS
        if (!duplicate) {
            lastKey = key
            lastShownAt = now
        }
        return duplicate
    }

    private fun missingCard(rawNumber: String): CachedBusinessCard {
        val displayNumber = rawNumber.ifBlank { "알 수 없는 번호" }
        return CachedBusinessCard(
            id = "${CachedBusinessCard.MISSING_CARD_ID_PREFIX}${displayNumber.hashCode()}",
            name = "등록된 명함 없음",
            company = "",
            department = "",
            position = "",
            email = "",
            mobile = displayNumber,
            phone = "",
            address = "",
            website = "",
            tags = "",
            memoPreview = "",
            overlayImageKind = "generated",
            imagePath = null,
        )
    }

    private const val DUPLICATE_WINDOW_MS = 4_000L
}
