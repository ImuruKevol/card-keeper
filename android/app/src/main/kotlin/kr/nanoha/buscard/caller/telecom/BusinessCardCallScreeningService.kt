package kr.nanoha.buscard.caller.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import kr.nanoha.buscard.caller.overlay.BusinessCardOverlayService

class BusinessCardCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val normalizedNumber = PhoneNumberNormalizer.aliases(rawNumber).firstOrNull().orEmpty()
        if (normalizedNumber.isBlank()) return

        val intent = Intent(this, BusinessCardOverlayService::class.java).apply {
            putExtra(BusinessCardOverlayService.EXTRA_DISPLAY_NUMBER, rawNumber)
            putExtra(BusinessCardOverlayService.EXTRA_NORMALIZED_NUMBER, normalizedNumber)
        }
        startService(intent)
    }
}
