package kr.nanoha.buscard.caller.overlay

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
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
import kr.nanoha.buscard.caller.data.CachedBusinessCard
import kr.nanoha.buscard.caller.data.ContactHistoryEntry
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

object BusinessCardDisplayController {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    fun show(context: Context, card: CachedBusinessCard, history: List<ContactHistoryEntry> = emptyList()) {
        val appContext = context.applicationContext
        val settings = OverlaySettingsStore(appContext).load()
        if (settings.displayMode == CardDisplayMode.NOTIFICATION || !Settings.canDrawOverlays(appContext)) {
            BusinessCardNotification.show(appContext, card, history)
            return
        }

        val render = {
            runCatching {
                showOverlay(appContext, card, settings, history)
            }.onFailure {
                BusinessCardNotification.show(appContext, card, history)
            }
            Unit
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            render()
        } else {
            handler.post(render)
        }
    }

    fun remove() {
        handler.removeCallbacksAndMessages(null)
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        windowManager = null
    }

    private fun showOverlay(context: Context, card: CachedBusinessCard, settings: OverlaySettings, history: List<ContactHistoryEntry>) {
        remove()
        windowManager = context.getSystemService(WindowManager::class.java)
        val view = overlayContent(context, card, settings, history)
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
        attachDragHandler(context, view, params)
        windowManager?.addView(view, params)
        handler.postDelayed({ remove() }, OVERLAY_TIMEOUT_MS)
    }

    private fun overlayContent(context: Context, card: CachedBusinessCard, settings: OverlaySettings, history: List<ContactHistoryEntry>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            alpha = settings.opacity
            elevation = dp(context, 12).toFloat()
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(context, 18).toFloat()
                setStroke(dp(context, 1), Color.rgb(214, 226, 222))
            }

            if (card.isMissingCard) {
                addView(missingCardHeader(context, card))
            } else {
                val screenWidth = context.resources.displayMetrics.widthPixels
                val imageWidth = (min(screenWidth * 0.78f, dp(context, 420).toFloat()) * settings.scale).roundToInt().coerceAtLeast(dp(context, 280))
                val imageHeight = (imageWidth * 680f / 1200f).roundToInt()
                val bitmap = card.imagePath
                    ?.let { path -> runCatching { if (File(path).exists()) BitmapFactory.decodeFile(path) else null }.getOrNull() }
                    ?: CardImageRenderer.render(card)

                addView(ImageView(context).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    contentDescription = "명함 이미지"
                }, LinearLayout.LayoutParams(imageWidth, imageHeight))

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(context, 8), 0, 0)
                    addView(TextView(context).apply {
                        text = listOf(card.name, card.primaryLine, card.displayPhone).filter { it.isNotBlank() }.joinToString(" · ")
                        textSize = 13f
                        setTextColor(Color.rgb(38, 49, 45))
                        maxLines = 1
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(closeButton(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 38)))
                })
            }

            if (history.isNotEmpty()) {
                addView(historyContent(context, history), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(context, 8)
                })
            }
        }
    }

    private fun missingCardHeader(context: Context, card: CachedBusinessCard): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = if (card.hasResolvedMissingName) card.name else CachedBusinessCard.MISSING_CARD_TITLE
                    textSize = 18f
                    setTextColor(Color.rgb(23, 32, 29))
                })
                val detail = listOf(
                    if (card.hasResolvedMissingName) CachedBusinessCard.MISSING_CARD_TITLE else "",
                    PhoneNumberNormalizer.display(card.displayPhone).ifBlank { card.displayPhone },
                ).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = detail
                        textSize = 13f
                        setTextColor(Color.rgb(87, 99, 93))
                        setPadding(0, dp(context, 4), 0, 0)
                    })
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(closeButton(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 38)))
        }
    }

    private fun closeButton(context: Context): Button {
        return Button(context).apply {
            text = "닫기"
            textSize = 12f
            minHeight = dp(context, 34)
            minimumHeight = dp(context, 34)
            setOnClickListener { remove() }
        }
    }

    private fun historyContent(context: Context, history: List<ContactHistoryEntry>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(248, 250, 247))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 1), Color.rgb(224, 231, 224))
            }
            addView(TextView(context).apply {
                text = "최근 기록"
                textSize = 12f
                setTextColor(Color.rgb(87, 99, 93))
            })
            history.take(5).forEach { item ->
                addView(TextView(context).apply {
                    text = item.displayText
                    textSize = 12f
                    setTextColor(Color.rgb(38, 49, 45))
                    maxLines = 1
                    setPadding(0, dp(context, 5), 0, 0)
                })
            }
        }
    }

    private fun attachDragHandler(context: Context, view: View, params: WindowManager.LayoutParams) {
        val settingsStore = OverlaySettingsStore(context)
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

    private fun isRightGravity(gravity: Int): Boolean {
        return ((gravity and Gravity.RIGHT) == Gravity.RIGHT) || ((gravity and Gravity.END) == Gravity.END)
    }

    private fun isBottomGravity(gravity: Int): Boolean {
        return (gravity and Gravity.BOTTOM) == Gravity.BOTTOM
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private const val OVERLAY_TIMEOUT_MS = 15_000L
}
