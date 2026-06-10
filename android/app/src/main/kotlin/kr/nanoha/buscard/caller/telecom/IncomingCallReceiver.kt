package kr.nanoha.buscard.caller.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE).orEmpty()
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        if (rawNumber.isBlank()) return
        IncomingCallerDisplay.show(context, rawNumber)
    }
}
