package kr.nanoha.buscard.caller.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class BusinessCardOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        showOverlay(
            displayNumber = intent?.getStringExtra(EXTRA_DISPLAY_NUMBER).orEmpty(),
            normalizedNumber = intent?.getStringExtra(EXTRA_NORMALIZED_NUMBER).orEmpty(),
        )
        handler.postDelayed({ stopSelf() }, OVERLAY_TIMEOUT_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay(displayNumber: String, normalizedNumber: String) {
        removeOverlay()
        windowManager = getSystemService(WindowManager::class.java)
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 22)
            setBackgroundColor(0xF2FFFFFF.toInt())
            addView(TextView(context).apply {
                text = "명함장"
                textSize = 13f
                setTextColor(0xFF64748B.toInt())
            })
            addView(TextView(context).apply {
                text = displayNumber.ifBlank { normalizedNumber }
                textSize = 20f
                setTextColor(Color.BLACK)
            })
            addView(TextView(context).apply {
                text = "명함 이미지 캐시 연동 예정"
                textSize = 13f
                setTextColor(0xFF475569.toInt())
            })
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 32
            y = 180
        }
        windowManager?.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
    }

    companion object {
        const val EXTRA_DISPLAY_NUMBER = "display_number"
        const val EXTRA_NORMALIZED_NUMBER = "normalized_number"
        private const val OVERLAY_TIMEOUT_MS = 15_000L
    }
}
