package kr.nanoha.buscard.caller.data

data class SyncPhoneNumber(
    val kind: String,
    val displayNumber: String,
    val aliases: List<String>,
)

data class SyncCard(
    val id: String,
    val deleted: Boolean,
    val updatedAt: String,
    val name: String,
    val company: String,
    val department: String,
    val position: String,
    val email: String,
    val mobile: String,
    val phone: String,
    val address: String,
    val website: String,
    val tags: String,
    val memoPreview: String,
    val phoneNumbers: List<SyncPhoneNumber>,
    val imageHashes: Map<String, String>,
    val overlayImageKind: String,
) {
    fun imageHash(kind: String = overlayImageKind): String = imageHashes[kind].orEmpty()
}

data class CachedBusinessCard(
    val id: String,
    val name: String,
    val company: String,
    val department: String,
    val position: String,
    val email: String,
    val mobile: String,
    val phone: String,
    val address: String,
    val website: String,
    val tags: String,
    val memoPreview: String,
    val overlayImageKind: String,
    val imagePath: String?,
) {
    val primaryLine: String
        get() = listOf(company, position).filter { it.isNotBlank() }.joinToString(" · ")

    val displayPhone: String
        get() = mobile.ifBlank { phone }

    val isMissingCard: Boolean
        get() = id.startsWith(MISSING_CARD_ID_PREFIX)

    val hasResolvedMissingName: Boolean
        get() = isMissingCard && name.isNotBlank() && name != MISSING_CARD_TITLE

    companion object {
        const val MISSING_CARD_ID_PREFIX = "missing-card:"
        const val MISSING_CARD_TITLE = "등록된 명함 없음"
    }
}

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val deviceId: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
)

data class SyncResult(
    val changedCards: Int,
    val downloadedImages: Int,
    val activeCards: Int,
    val serverTime: String,
)
