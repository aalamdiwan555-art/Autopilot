package com.mamabhutnika.rideaccepter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class RemoteUser(
    val uid: String,
    val name: String,
    val email: String,
    val referralCode: String,
    val subscriptionUntilMs: Long,
    val subscriptionActive: Boolean,
    val rewardProgress: Int,
    val totalAdsWatched: Int,
    val isAdmin: Boolean,
)

data class AuthResult(val token: String, val user: RemoteUser)

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

class ApiClient {

    companion object {
        private const val REQUEST_TIMEOUT_MS = 12_000

        private fun userFromJson(json: JSONObject): RemoteUser {
            return RemoteUser(
                uid = json.optString("uid"),
                name = json.optString("name"),
                email = json.optString("email"),
                referralCode = json.optString("referralCode"),
                subscriptionUntilMs = parseDateMs(json.optString("subscriptionUntil")),
                subscriptionActive = json.optBoolean("subscriptionActive"),
                rewardProgress = json.optInt("rewardProgress"),
                totalAdsWatched = json.optInt("totalAdsWatched"),
                isAdmin = json.optBoolean("isAdmin"),
            )
        }

        private fun parseDateMs(value: String): Long {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
            )
            for (pattern in formats) {
                try {
                    return SimpleDateFormat(pattern, Locale.US).parse(value)?.time ?: 0L
                } catch (_: Exception) {
                    // Try the next valid ISO-8601 shape.
                }
            }
            return 0L
        }
    }

    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") &&
            !baseUrl.contains("api.invalid", ignoreCase = true)

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        referralCode: String,
    ): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password)
            .put("referralCode", referralCode)
        authResult(request("POST", "/mobile/auth/signup", body))
    }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
        authResult(request("POST", "/mobile/auth/login", body))
    }

    suspend fun me(token: String): RemoteUser = withContext(Dispatchers.IO) {
        userFromJson(request("GET", "/mobile/me", token = token).getJSONObject("user"))
    }

    suspend fun redeemReferral(token: String, referralCode: String): String =
        withContext(Dispatchers.IO) {
            request(
                "POST",
                "/mobile/referrals/redeem",
                JSONObject().put("referralCode", referralCode),
                token,
            ).optString("message", "Referral code accepted.")
        }

    suspend fun recordAdCompleted(token: String): RemoteUser = withContext(Dispatchers.IO) {
        userFromJson(
            request("POST", "/mobile/rewards/ad-completed", token = token)
                .getJSONObject("user"),
        )
    }

    suspend fun adminUsers(token: String): List<RemoteUser> = withContext(Dispatchers.IO) {
        val users = request("GET", "/mobile/admin/users", token = token).getJSONArray("users")
        List(users.length()) { index -> userFromJson(users.getJSONObject(index)) }
    }

    suspend fun grantDays(token: String, uid: String, days: Int): RemoteUser =
        withContext(Dispatchers.IO) {
            userFromJson(
                request(
                    "POST",
                    "/mobile/admin/users/${java.net.URLEncoder.encode(uid, "UTF-8")}/grant",
                    JSONObject().put("days", days),
                    token,
                ).getJSONObject("user"),
            )
        }

    private fun authResult(json: JSONObject): AuthResult {
        return AuthResult(
            token = json.getString("token"),
            user = userFromJson(json.getJSONObject("user")),
        )
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
    ): JSONObject {
        if (!isConfigured) {
            throw ApiException(
                "Account services are not configured in this build. Set API_BASE_URL to the published API ending in /api.",
            )
        }
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(
                InputStreamReader(stream ?: ByteArrayInputStream(ByteArray(0))),
            ).use { it.readText() }
            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject().put("error", "Unexpected server response.")
            }
            if (status !in 200..299) {
                throw ApiException(json.optString("error", "Request failed ($status)."), status)
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}