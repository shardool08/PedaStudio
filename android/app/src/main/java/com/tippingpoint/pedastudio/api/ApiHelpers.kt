package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.data.TeacherAccount
import org.json.JSONObject

internal fun parseApiError(text: String, code: Int): Exception {
    val json = runCatching { JSONObject(text) }.getOrNull()
    val serverError = json?.optString("error", text)?.takeIf { it.isNotBlank() } ?: text
    val friendly = when (code) {
        401 -> "Please sign in again."
        403, 429 -> serverError.ifBlank { "This feature needs a higher plan." }
        else -> serverError.ifBlank { "Server error $code" }
    }
    return Exception(friendly)
}

internal fun parseAccountResponse(root: JSONObject): Pair<JSONObject?, TeacherAccount?> {
    val payload = root.optJSONObject("account") ?: root
    val account = if (payload.has("tier")) AccountApiClient.parseAccount(payload) else null
    val inner = when {
        root.has("worksheet") -> root.optJSONObject("worksheet")
        root.has("analysis") -> root.optJSONObject("analysis")
        root.has("kit") -> root.optJSONObject("kit")
        root.has("catalog") -> root
        root.has("record") -> root
        else -> null
    }
    return inner to account
}
