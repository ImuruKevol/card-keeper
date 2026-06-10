package kr.nanoha.buscard.caller.telecom

import android.telecom.Call
import android.telecom.CallScreeningService

class BusinessCardCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

            val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
            IncomingCallerDisplay.show(this, rawNumber)
        } finally {
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }
}
