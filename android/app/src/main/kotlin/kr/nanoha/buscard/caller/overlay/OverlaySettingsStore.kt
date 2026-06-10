package kr.nanoha.buscard.caller.overlay

import android.content.Context
import android.view.Gravity
import kotlin.math.roundToInt

data class OverlaySettings(
    val gravity: Int,
    val offsetX: Int,
    val offsetY: Int,
    val scale: Float,
    val opacity: Float,
    val displayMode: CardDisplayMode,
)

enum class CardDisplayMode(val storageValue: String) {
    OVERLAY("overlay"),
    NOTIFICATION("notification");

    companion object {
        fun from(value: String?): CardDisplayMode {
            return values().firstOrNull { it.storageValue == value } ?: OVERLAY
        }
    }
}

class OverlaySettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("overlay-settings", Context.MODE_PRIVATE)

    fun load(): OverlaySettings {
        return OverlaySettings(
            gravity = prefs.getInt(KEY_GRAVITY, Gravity.TOP or Gravity.END),
            offsetX = prefs.getInt(KEY_X, dp(24)),
            offsetY = prefs.getInt(KEY_Y, dp(140)),
            scale = prefs.getFloat(KEY_SCALE, 1.0f).coerceIn(0.8f, 1.2f),
            opacity = prefs.getFloat(KEY_OPACITY, 0.98f).coerceIn(0.85f, 1.0f),
            displayMode = CardDisplayMode.from(prefs.getString(KEY_DISPLAY_MODE, null)),
        )
    }

    fun savePosition(gravity: Int, x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_GRAVITY, gravity)
            .putInt(KEY_X, x)
            .putInt(KEY_Y, y)
            .apply()
    }

    fun saveScale(scale: Float) {
        prefs.edit().putFloat(KEY_SCALE, scale.coerceIn(0.8f, 1.2f)).apply()
    }

    fun saveOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_OPACITY, opacity.coerceIn(0.85f, 1.0f)).apply()
    }

    fun saveDisplayMode(mode: CardDisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.storageValue).apply()
    }

    fun presetTopEnd() {
        savePosition(Gravity.TOP or Gravity.END, dp(24), dp(140))
    }

    fun presetBottomEnd() {
        savePosition(Gravity.BOTTOM or Gravity.END, dp(24), dp(170))
    }

    fun presetCenter() {
        savePosition(Gravity.CENTER, 0, 0)
    }

    private fun dp(value: Int): Int = (value * appContext.resources.displayMetrics.density).roundToInt()

    companion object {
        private const val KEY_GRAVITY = "gravity"
        private const val KEY_X = "offset_x"
        private const val KEY_Y = "offset_y"
        private const val KEY_SCALE = "scale"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_DISPLAY_MODE = "display_mode"
    }
}
