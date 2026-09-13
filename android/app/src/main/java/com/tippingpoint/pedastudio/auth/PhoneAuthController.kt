package com.tippingpoint.pedastudio.auth

import android.app.Activity
import android.content.Context
import com.tippingpoint.pedastudio.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Sending : LoginUiState()
    data object CodeSent : LoginUiState()
    data object Verifying : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * Phone OTP sign-in.
 *
 * Pilot mode (`BuildConfig.USE_PILOT_OTP`): talks to `/api/auth/pilot-login` — no SMS provider.
 * Production mode: Supabase Auth `/auth/v1/otp` + `/auth/v1/verify` (needs Textlocal/Twilio).
 */
class PhoneAuthController(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var pendingPhone: String? = prefs.getString(KEY_PHONE, null)

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    val userId: String? get() = prefs.getString(KEY_USER_ID, null)

    val isLoggedIn: Boolean get() = !prefs.getString(KEY_REFRESH, null).isNullOrBlank()

    suspend fun getIdToken(): String? {
        val access = prefs.getString(KEY_ACCESS, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES, 0L)
        if (!access.isNullOrBlank() && expiresAt > System.currentTimeMillis() + 30_000L) {
            return access
        }
        return refreshSession()
    }

    fun sendOtp(@Suppress("UNUSED_PARAMETER") activity: Activity, phoneDigits: String) {
        if (phoneDigits.length != 10) {
            _state.value = LoginUiState.Error("Enter a valid 10-digit mobile number")
            return
        }
        val phone = "+91$phoneDigits"
        _state.value = LoginUiState.Sending
        scope.launch {
            try {
                if (BuildConfig.USE_PILOT_OTP) {
                    // No SMS — OTP is the fixed pilot code from the server (default 123456).
                    pendingPhone = phone
                    prefs.edit().putString(KEY_PHONE, phone).apply()
                    _state.value = LoginUiState.CodeSent
                    return@launch
                }
                postSupabase("/auth/v1/otp", JSONObject().put("phone", phone))
                pendingPhone = phone
                prefs.edit().putString(KEY_PHONE, phone).apply()
                _state.value = LoginUiState.CodeSent
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(friendlyMessage(e))
            }
        }
    }

    fun verifyOtp(code: String) {
        val phone = pendingPhone
        if (phone.isNullOrBlank()) {
            _state.value = LoginUiState.Error("Request OTP again")
            return
        }
        if (code.length < 6) {
            _state.value = LoginUiState.Error("Enter 6-digit OTP")
            return
        }
        _state.value = LoginUiState.Verifying
        scope.launch {
            try {
                val session = if (BuildConfig.USE_PILOT_OTP) {
                    postApi(
                        "/api/auth/pilot-login",
                        JSONObject()
                            .put("phone", phone)
                            .put("otp", code),
                    )
                } else {
                    postSupabase(
                        "/auth/v1/verify",
                        JSONObject()
                            .put("phone", phone)
                            .put("token", code)
                            .put("type", "sms"),
                    )
                }
                persistSession(session)
                _state.value = LoginUiState.Idle
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(friendlyMessage(e))
            }
        }
    }

    fun resetError() {
        if (_state.value is LoginUiState.Error) {
            _state.value = LoginUiState.Idle
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        pendingPhone = null
        _state.value = LoginUiState.Idle
    }

    private fun refreshSession(): String? {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        return runCatching {
            val session = postSupabase(
                "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh),
            )
            persistSession(session)
            session.optString("access_token").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun persistSession(session: JSONObject) {
        val access = session.optString("access_token")
        val refresh = session.optString("refresh_token")
        val expiresIn = session.optLong("expires_in", 3600L)
        val userId = session.optJSONObject("user")?.optString("id").orEmpty()
        if (access.isBlank() || refresh.isBlank()) {
            throw IllegalStateException("Sign-in did not return a session")
        }
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putLong(KEY_EXPIRES, System.currentTimeMillis() + expiresIn * 1000L)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    private fun postApi(path: String, body: JSONObject): JSONObject {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        if (base.isBlank()) throw IllegalStateException("API URL not configured")
        val request = Request.Builder()
            .url("$base$path")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        return execute(request)
    }

    private fun postSupabase(path: String, body: JSONObject): JSONObject {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (base.isBlank() || key.isBlank()) {
            throw IllegalStateException("Supabase is not configured in this build")
        }
        val request = Request.Builder()
            .url("$base$path")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): JSONObject {
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val json = runCatching { JSONObject(text) }.getOrNull()
                val server = json?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: json?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: json?.optString("error_description")?.takeIf { it.isNotBlank() }
                    ?: ""
                throw IllegalStateException(server.ifBlank { "HTTP ${response.code}" })
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private fun friendlyMessage(e: Exception): String {
        val message = e.message.orEmpty()
        return when {
            message.contains("invalid", true) && message.contains("phone", true) ->
                "Invalid mobile number"
            message.contains("rate", true) || message.contains("too many", true) ->
                "Too many attempts. Wait and try again."
            message.contains("otp", true) || message.contains("token", true) ||
                message.contains("Invalid OTP", true) ->
                "Invalid OTP"
            message.contains("unsupported phone", true) || message.contains("phone provider", true) ->
                "Phone SMS is not set up yet. Use pilot login (OTP 123456) or add Textlocal."
            message.contains("Pilot OTP is off", true) ->
                "Pilot OTP is off on the server. Set ALLOW_DEV_OTP=true in Railway."
            message.contains("not configured", true) ->
                "App is missing Supabase settings. Rebuild after adding them to local.properties."
            else -> message.ifBlank { "Could not sign in. Try again." }
        }
    }

    companion object {
        private const val PREFS = "pedastudio_auth"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE = "phone"
        private val JSON = "application/json".toMediaType()
    }
}
