package kr.nanoha.buscard.caller.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kr.nanoha.buscard.caller.data.AuthSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class MobileTokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mobile-auth", Context.MODE_PRIVATE)

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(session.accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(session.refreshToken))
            .putString(KEY_DEVICE_ID, session.deviceId)
            .putString(KEY_ACCESS_EXPIRES, session.accessExpiresAt)
            .putString(KEY_REFRESH_EXPIRES, session.refreshExpiresAt)
            .apply()
    }

    fun accessToken(): String = decrypt(prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty())

    fun refreshToken(): String = decrypt(prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty())

    fun deviceId(): String = prefs.getString(KEY_DEVICE_ID, "").orEmpty()

    fun lastSyncAt(): String = prefs.getString(KEY_LAST_SYNC_AT, "").orEmpty()

    fun setLastSyncAt(value: String) {
        prefs.edit().putString(KEY_LAST_SYNC_AT, value).apply()
    }

    fun isLoggedIn(): Boolean = accessToken().isNotBlank() || refreshToken().isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        if (value.isBlank()) return ""
        return try {
            val packed = Base64.decode(value, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, GCM_IV_BYTES)
            val payload = packed.copyOfRange(GCM_IV_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "business-card-caller-mobile-token"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACCESS_EXPIRES = "access_expires"
        private const val KEY_REFRESH_EXPIRES = "refresh_expires"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
    }
}
