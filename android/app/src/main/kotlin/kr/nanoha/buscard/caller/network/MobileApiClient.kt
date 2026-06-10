package kr.nanoha.buscard.caller.network

import android.util.Base64
import kr.nanoha.buscard.caller.BuildConfig
import kr.nanoha.buscard.caller.data.AuthSession
import kr.nanoha.buscard.caller.data.SyncCard
import kr.nanoha.buscard.caller.data.SyncPhoneNumber
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MobileApiException(val statusCode: Int, message: String) : Exception(message)

data class DownloadedImage(
    val data: ByteArray,
    val mime: String,
    val hash: String,
)

data class ImageDownloadRequest(
    val cardId: String,
    val kind: String,
)

data class DownloadedCardImage(
    val cardId: String,
    val requestedKind: String,
    val kind: String,
    val data: ByteArray,
    val mime: String,
    val hash: String,
    val missing: Boolean,
)

data class SyncPayload(
    val cards: List<SyncCard>,
    val serverTime: String,
)

class MobileApiClient(private val baseUrl: String = BuildConfig.WEB_BASE_URL.trimEnd('/')) {
    fun login(email: String, password: String, deviceName: String): AuthSession {
        val json = postForm(
            "/api/mobile/auth/login",
            mapOf(
                "email" to email,
                "password" to password,
                "device_name" to deviceName,
                "platform" to "android",
            ),
        )
        return json.toAuthSession()
    }

    fun refresh(refreshToken: String): AuthSession {
        val json = postForm("/api/mobile/auth/refresh", mapOf("refresh_token" to refreshToken))
        return json.toAuthSession()
    }

    fun logout(accessToken: String) {
        postForm("/api/mobile/auth/logout", emptyMap(), accessToken)
    }

    fun sync(accessToken: String, since: String): SyncPayload {
        val query = if (since.isBlank()) "" else "?since=${encode(since)}"
        val json = requestJson("GET", "/api/mobile/cards/sync$query", accessToken)
        val cardsJson = json.optJSONArray("cards")
        val cards = buildList {
            if (cardsJson != null) {
                for (index in 0 until cardsJson.length()) {
                    add(cardsJson.getJSONObject(index).toSyncCard())
                }
            }
        }
        return SyncPayload(cards = cards, serverTime = json.optString("server_time"))
    }

    fun downloadOverlayImage(accessToken: String, cardId: String, kind: String): DownloadedImage? {
        val connection = openConnection(
            method = "GET",
            path = "/api/mobile/cards/${encodePath(cardId)}/overlay-image?kind=${encode(kind)}",
            accessToken = accessToken,
        )
        connection.connect()
        val code = connection.responseCode
        if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
        if (code !in 200..299) throw MobileApiException(code, readError(connection))
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input -> input.copyTo(output) }
        return DownloadedImage(
            data = output.toByteArray(),
            mime = connection.contentType?.substringBefore(';')?.ifBlank { "image/png" } ?: "image/png",
            hash = connection.getHeaderField("X-Image-Hash").orEmpty(),
        )
    }

    fun downloadOverlayImages(accessToken: String, requests: List<ImageDownloadRequest>): List<DownloadedCardImage> {
        if (requests.isEmpty()) return emptyList()
        val items = JSONArray()
        requests.forEach { request ->
            items.put(JSONObject().apply {
                put("card_id", request.cardId)
                put("kind", request.kind)
            })
        }
        val json = postJson(
            "/api/mobile/cards/overlay-images",
            JSONObject().put("items", items),
            accessToken,
        )
        val imagesJson = json.optJSONArray("images") ?: JSONArray()
        return buildList {
            for (index in 0 until imagesJson.length()) {
                val item = imagesJson.optJSONObject(index) ?: continue
                add(item.toDownloadedCardImage())
            }
        }
    }

    private fun postForm(path: String, fields: Map<String, String>, accessToken: String = ""): JSONObject {
        val body = fields.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }.toByteArray(Charsets.UTF_8)
        val connection = openConnection("POST", path, accessToken)
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.doOutput = true
        connection.outputStream.use { it.write(body) }
        return readJson(connection)
    }

    private fun postJson(path: String, body: JSONObject, accessToken: String = ""): JSONObject {
        val connection = openConnection("POST", path, accessToken)
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        return readJson(connection)
    }

    private fun requestJson(method: String, path: String, accessToken: String = ""): JSONObject {
        val connection = openConnection(method, path, accessToken)
        return readJson(connection)
    }

    private fun openConnection(method: String, path: String, accessToken: String = ""): HttpURLConnection {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/json,image/*")
        connection.setRequestProperty("User-Agent", "BusinessCardCaller/0.1 Android")
        if (accessToken.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return connection
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val text = if (code in 200..299) {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            readError(connection)
        }
        if (code !in 200..299) {
            val message = runCatching { JSONObject(text).optString("message") }.getOrDefault(text).ifBlank { "API 요청에 실패했습니다." }
            throw MobileApiException(code, message)
        }
        val root = JSONObject(text)
        if (root.has("code") && root.has("data")) {
            val wrappedCode = root.optInt("code", code)
            val data = root.optJSONObject("data") ?: JSONObject()
            if (wrappedCode !in 200..299) {
                val message = data.optString("message").ifBlank { "API 요청에 실패했습니다." }
                throw MobileApiException(wrappedCode, message)
            }
            return data
        }
        return root
    }

    private fun readError(connection: HttpURLConnection): String {
        return try {
            val stream = connection.errorStream ?: return ""
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    private fun JSONObject.toAuthSession(): AuthSession {
        val device = optJSONObject("device") ?: JSONObject()
        return AuthSession(
            accessToken = optString("access_token"),
            refreshToken = optString("refresh_token"),
            deviceId = device.optString("id"),
            accessExpiresAt = device.optString("access_expires"),
            refreshExpiresAt = device.optString("refresh_expires"),
        )
    }

    private fun JSONObject.toSyncCard(): SyncCard {
        val phoneArray = optJSONArray("phone_numbers")
        val phones = buildList {
            if (phoneArray != null) {
                for (index in 0 until phoneArray.length()) {
                    val item = phoneArray.getJSONObject(index)
                    val aliasArray = item.optJSONArray("aliases")
                    val aliases = buildList {
                        if (aliasArray != null) {
                            for (aliasIndex in 0 until aliasArray.length()) add(aliasArray.optString(aliasIndex))
                        }
                    }.filter { it.isNotBlank() }
                    add(SyncPhoneNumber(item.optString("kind"), item.optString("display_number"), aliases))
                }
            }
        }
        val hashes = optJSONObject("image_hashes") ?: JSONObject()
        return SyncCard(
            id = optString("id"),
            deleted = optBoolean("deleted", false),
            updatedAt = optString("updated_at"),
            name = optString("name"),
            company = optString("company"),
            department = optString("department"),
            position = optString("position"),
            email = optString("email"),
            mobile = optString("mobile"),
            phone = optString("phone"),
            address = optString("address"),
            website = optString("website"),
            tags = optString("tags"),
            memoPreview = optString("memo_preview"),
            phoneNumbers = phones,
            imageHashes = mapOf(
                "front" to hashes.optString("front"),
                "back" to hashes.optString("back"),
                "generated" to hashes.optString("generated"),
            ),
            overlayImageKind = optString("overlay_image_kind", "generated"),
        )
    }

    private fun JSONObject.toDownloadedCardImage(): DownloadedCardImage {
        val encoded = optString("data")
        val data = if (encoded.isBlank()) ByteArray(0) else Base64.decode(encoded, Base64.DEFAULT)
        return DownloadedCardImage(
            cardId = optString("card_id"),
            requestedKind = optString("requested_kind"),
            kind = optString("kind"),
            data = data,
            mime = optString("mime", "image/png"),
            hash = optString("hash"),
            missing = optBoolean("missing", false),
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encodePath(value: String): String = encode(value).replace("+", "%20")
}
