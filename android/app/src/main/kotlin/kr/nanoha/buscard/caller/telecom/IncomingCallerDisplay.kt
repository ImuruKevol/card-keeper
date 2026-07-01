package kr.nanoha.buscard.caller.telecom

import android.content.Context
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import kr.nanoha.buscard.caller.data.BusinessCardDatabase
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.CallerNameResolver
import kr.nanoha.buscard.caller.data.ContactHistoryReader
import kr.nanoha.buscard.caller.overlay.BusinessCardNotification
import kr.nanoha.buscard.caller.overlay.OverlaySettingsStore

object IncomingCallerDisplay {
    private var lastKey = ""
    private var lastShownAt = 0L

    fun show(context: Context, rawNumber: String): Boolean {
        val appContext = context.applicationContext
        val aliases = PhoneNumberNormalizer.aliases(rawNumber)
        if (aliases.isEmpty()) return false

        val key = aliases.sorted().joinToString("|")
        if (isDuplicate(key)) return true

        val savedCard = BusinessCardDatabase(appContext).findByPhoneAliases(aliases)
        if (savedCard == null && !OverlaySettingsStore(appContext).load().showMissingCardAlerts) {
            return false
        }

        val card = savedCard ?: missingCard(appContext, rawNumber, aliases)
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

    private fun missingCard(context: Context, rawNumber: String, aliases: Set<String>): CachedBusinessCard {
        val displayNumber = PhoneNumberNormalizer.display(rawNumber).ifBlank { rawNumber.ifBlank { "알 수 없는 번호" } }
        val resolvedName = CallerNameResolver.resolveName(context, rawNumber, aliases)
        return CachedBusinessCard(
            id = "${CachedBusinessCard.MISSING_CARD_ID_PREFIX}${displayNumber.hashCode()}",
            name = resolvedName.ifBlank { CachedBusinessCard.MISSING_CARD_TITLE },
            company = if (resolvedName.isNotBlank()) CachedBusinessCard.MISSING_CARD_TITLE else "",
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
