package kr.nanoha.buscard.caller.sync

import android.content.Context
import android.os.Build
import kr.nanoha.buscard.caller.auth.MobileTokenStore
import kr.nanoha.buscard.caller.data.BusinessCardDatabase
import kr.nanoha.buscard.caller.data.SyncCard
import kr.nanoha.buscard.caller.data.SyncResult
import kr.nanoha.buscard.caller.network.DownloadedCardImage
import kr.nanoha.buscard.caller.network.ImageDownloadRequest
import kr.nanoha.buscard.caller.network.MobileApiClient
import kr.nanoha.buscard.caller.network.MobileApiException
import java.io.File

data class SyncProgress(
    val message: String,
    val current: Int = 0,
    val total: Int = 0,
) {
    val isIndeterminate: Boolean
        get() = total <= 0

    val percent: Int
        get() = if (isIndeterminate) 0 else ((current.coerceIn(0, total) * 100f) / total).toInt().coerceIn(0, 100)
}

class BusinessCardSyncManager(context: Context) {
    private val appContext = context.applicationContext
    private val api = MobileApiClient()
    private val db = BusinessCardDatabase(appContext)
    private val tokenStore = MobileTokenStore(appContext)
    private val syncPrefs = appContext.getSharedPreferences("business-card-sync", Context.MODE_PRIVATE)

    fun loginAndSync(email: String, password: String, onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        onProgress(SyncProgress("로그인 중입니다."))
        val session = api.login(email, password, deviceName())
        tokenStore.save(session)
        tokenStore.setLastSyncAt("")
        return syncWithAccessToken(session.accessToken, onProgress = onProgress)
    }

    fun syncNow(onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        val accessToken = tokenStore.accessToken()
        if (accessToken.isBlank()) {
            return refreshThenSync(onProgress = onProgress)
        }
        return try {
            syncWithAccessToken(accessToken, onProgress = onProgress)
        } catch (error: MobileApiException) {
            if (error.statusCode == 401) refreshThenSync(onProgress = onProgress) else throw error
        }
    }

    fun repairImagesNow(onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        val accessToken = tokenStore.accessToken()
        if (accessToken.isBlank()) {
            return refreshThenSync(forceImageRepair = true, onProgress = onProgress)
        }
        return try {
            syncWithAccessToken(accessToken, forceImageRepair = true, onProgress = onProgress)
        } catch (error: MobileApiException) {
            if (error.statusCode == 401) refreshThenSync(forceImageRepair = true, onProgress = onProgress) else throw error
        }
    }

    fun logout() {
        val accessToken = tokenStore.accessToken()
        if (accessToken.isNotBlank()) runCatching { api.logout(accessToken) }
        tokenStore.clear()
    }

    private fun refreshThenSync(forceImageRepair: Boolean = false, onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        val refreshToken = tokenStore.refreshToken()
        if (refreshToken.isBlank()) throw IllegalStateException("로그인이 필요합니다.")
        onProgress(SyncProgress("인증을 갱신하는 중입니다."))
        val session = api.refresh(refreshToken)
        tokenStore.save(session)
        return syncWithAccessToken(session.accessToken, forceImageRepair, onProgress)
    }

    private fun syncWithAccessToken(accessToken: String, forceImageRepair: Boolean = false, onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        val repairImageCache = forceImageRepair || syncPrefs.getInt(KEY_IMAGE_CACHE_VERSION, 0) < IMAGE_CACHE_VERSION
        onProgress(SyncProgress(if (repairImageCache) "명함과 이미지를 전체 확인하는 중입니다." else "변경된 명함을 확인하는 중입니다."))
        val payload = api.sync(accessToken, if (repairImageCache) "" else tokenStore.lastSyncAt())
        val total = payload.cards.size
        if (total == 0) onProgress(SyncProgress("변경된 명함이 없습니다.", 1, 1))
        payload.cards.forEachIndexed { index, card ->
            onProgress(SyncProgress("명함 데이터를 저장하는 중입니다.", index, total))
            db.replaceCard(card)
            onProgress(SyncProgress("명함 데이터를 저장하는 중입니다.", index + 1, total))
        }
        val downloaded = syncOverlayImagesIfNeeded(accessToken, payload.cards, payload.serverTime, repairImageCache, onProgress)
        onProgress(SyncProgress("동기화를 마무리하는 중입니다.", total.coerceAtLeast(1), total.coerceAtLeast(1)))
        if (payload.serverTime.isNotBlank()) {
            tokenStore.setLastSyncAt(payload.serverTime)
            if (repairImageCache) {
                syncPrefs.edit().putInt(KEY_IMAGE_CACHE_VERSION, IMAGE_CACHE_VERSION).apply()
            }
        }
        return SyncResult(
            changedCards = payload.cards.size,
            downloadedImages = downloaded,
            activeCards = db.activeCardCount(),
            serverTime = payload.serverTime,
        )
    }

    private fun syncOverlayImagesIfNeeded(
        accessToken: String,
        cards: List<SyncCard>,
        updatedAt: String,
        force: Boolean,
        onProgress: (SyncProgress) -> Unit,
    ): Int {
        val targets = cards.mapNotNull { card ->
            if (card.deleted) return@mapNotNull null
            val kind = card.overlayImageKind.ifBlank { "generated" }
            val expectedHash = card.imageHash(kind)
            if (expectedHash.isBlank()) {
                db.deleteOverlayImage(card.id, kind)
                return@mapNotNull null
            }
            if (!force && db.overlayImageHash(card.id, kind) == expectedHash) {
                return@mapNotNull null
            }
            ImageTarget(card, kind, expectedHash)
        }
        if (targets.isEmpty()) return 0

        onProgress(SyncProgress("명함 이미지 ${targets.size}건을 한 번에 내려받는 중입니다."))
        val images = api.downloadOverlayImages(
            accessToken,
            targets.map { target -> ImageDownloadRequest(target.card.id, target.kind) },
        ).associateBy { image -> image.cardId to image.requestedKind.ifBlank { image.kind } }

        var downloaded = 0
        targets.forEachIndexed { index, target ->
            val image = images[target.card.id to target.kind]
            if (image == null || !saveOverlayImage(target, image, updatedAt)) {
                db.deleteOverlayImage(target.card.id, target.kind)
            } else {
                downloaded += 1
            }
            onProgress(SyncProgress("명함 이미지를 저장하는 중입니다.", index + 1, targets.size))
        }
        return downloaded
    }

    private fun saveOverlayImage(target: ImageTarget, image: DownloadedCardImage, updatedAt: String): Boolean {
        if (image.missing || image.data.isEmpty()) return false
        val returnedKind = image.kind.ifBlank { target.kind }
        if (returnedKind != target.kind) return false
        val finalHash = image.hash.ifBlank { target.expectedHash }
        if (finalHash != target.expectedHash) return false

        val dir = File(appContext.filesDir, "overlay-images").apply { mkdirs() }
        val file = File(dir, "${target.card.id}-${target.kind}.${extensionFor(image.mime)}")
        file.writeBytes(image.data)
        db.upsertOverlayImage(target.card.id, target.kind, finalHash, file.absolutePath, image.mime, updatedAt.ifBlank { target.card.updatedAt })
        return true
    }

    private data class ImageTarget(
        val card: SyncCard,
        val kind: String,
        val expectedHash: String,
    )

    private fun extensionFor(mime: String): String {
        return when {
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("webp") -> "webp"
            else -> "png"
        }
    }

    private fun deviceName(): String {
        return listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android" }
    }

    companion object {
        private const val KEY_IMAGE_CACHE_VERSION = "image_cache_version"
        private const val IMAGE_CACHE_VERSION = 3
    }
}
