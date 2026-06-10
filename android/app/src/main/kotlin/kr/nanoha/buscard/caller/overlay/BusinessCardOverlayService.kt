package kr.nanoha.buscard.caller.overlay

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kr.nanoha.buscard.caller.core.PhoneNumberNormalizer
import kr.nanoha.buscard.caller.data.BusinessCardDatabase
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.ContactHistoryReader
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

class BusinessCardOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var settingsStore: OverlaySettingsStore

    override fun onCreate() {
        super.onCreate()
        settingsStore = OverlaySettingsStore(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val card = resolveCard(intent)
        if (card == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val aliases = PhoneNumberNormalizer.aliases(card.displayPhone)
        val history = ContactHistoryReader.recent(this, aliases)
        BusinessCardDisplayController.show(this, card, history)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        super.onDestroy()
    }

    private fun resolveCard(intent: Intent?): CachedBusinessCard? {
        if (intent?.getBooleanExtra(EXTRA_SAMPLE_CARD, false) == true) return sampleCard()
        val db = BusinessCardDatabase(this)
        val forcedCardId = intent?.getStringExtra(EXTRA_CARD_ID).orEmpty()
        if (forcedCardId.isNotBlank()) return db.getCard(forcedCardId) ?: sampleCard()

        val rawNumber = intent?.getStringExtra(EXTRA_DISPLAY_NUMBER).orEmpty()
        val normalizedNumber = intent?.getStringExtra(EXTRA_NORMALIZED_NUMBER).orEmpty()
        val aliases = linkedSetOf<String>().apply {
            addAll(PhoneNumberNormalizer.aliases(rawNumber))
            addAll(PhoneNumberNormalizer.aliases(normalizedNumber))
        }
        return db.findByPhoneAliases(aliases)
    }

    private fun showOverlay(card: CachedBusinessCard, settings: OverlaySettings) {
        removeOverlay()
        windowManager = getSystemService(WindowManager::class.java)
        val view = overlayContent(card, settings)
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = settings.gravity
            x = settings.offsetX
            y = settings.offsetY
        }
        attachDragHandler(view, params)
        windowManager?.addView(view, params)
    }

    private fun overlayContent(card: CachedBusinessCard, settings: OverlaySettings): LinearLayout {
        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (min(screenWidth * 0.78f, dp(420).toFloat()) * settings.scale).roundToInt().coerceAtLeast(dp(280))
        val imageHeight = (imageWidth * 680f / 1200f).roundToInt()
        val bitmap = card.imagePath
            ?.let { path -> runCatching { if (File(path).exists()) BitmapFactory.decodeFile(path) else null }.getOrNull() }
            ?: CardImageRenderer.render(card)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = settings.opacity
            elevation = dp(12).toFloat()
            setPadding(dp(12), dp(12), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(214, 226, 222))
            }

            addView(ImageView(context).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                contentDescription = "명함 이미지"
            }, LinearLayout.LayoutParams(imageWidth, imageHeight))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
                addView(TextView(context).apply {
                    text = listOf(card.name, card.primaryLine, card.displayPhone).filter { it.isNotBlank() }.joinToString(" · ")
                    textSize = 13f
                    setTextColor(Color.rgb(38, 49, 45))
                    maxLines = 1
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(Button(context).apply {
                    text = "닫기"
                    textSize = 12f
                    minHeight = dp(34)
                    minimumHeight = dp(34)
                    setOnClickListener { stopSelf() }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)))
            })
        }
    }

    private fun attachDragHandler(view: View, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).roundToInt()
                    val dy = (event.rawY - downRawY).roundToInt()
                    params.x = if (isRightGravity(params.gravity)) (startX - dx).coerceAtLeast(0) else (startX + dx).coerceAtLeast(0)
                    params.y = if (isBottomGravity(params.gravity)) (startY - dy).coerceAtLeast(0) else (startY + dy).coerceAtLeast(0)
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    settingsStore.savePosition(params.gravity, params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
    }

    private fun isRightGravity(gravity: Int): Boolean {
        return ((gravity and Gravity.RIGHT) == Gravity.RIGHT) || ((gravity and Gravity.END) == Gravity.END)
    }

    private fun isBottomGravity(gravity: Int): Boolean {
        return (gravity and Gravity.BOTTOM) == Gravity.BOTTOM
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun sampleCard(): CachedBusinessCard {
        return CachedBusinessCard(
            id = "sample",
            name = "홍길동",
            company = "명함장",
            department = "모바일",
            position = "담당자",
            email = "hello@bus.sub.nanoha.kr",
            mobile = "010-1234-5678",
            phone = "02-123-4567",
            address = "Seoul, Korea",
            website = "bus.sub.nanoha.kr",
            tags = "",
            memoPreview = "",
            overlayImageKind = "generated",
            imagePath = null,
        )
    }

    companion object {
        const val EXTRA_DISPLAY_NUMBER = "display_number"
        const val EXTRA_NORMALIZED_NUMBER = "normalized_number"
        const val EXTRA_CARD_ID = "card_id"
        const val EXTRA_SAMPLE_CARD = "sample_card"
        private const val OVERLAY_TIMEOUT_MS = 15_000L
    }
}
