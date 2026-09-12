package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.BuildConfig
import com.tippingpoint.pedastudio.data.TeacherAccount
import com.tippingpoint.pedastudio.data.TierConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SubscriptionApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun base(): String = BuildConfig.API_BASE_URL.trimEnd('/')

    fun fetchCatalog(): Result<SubscriptionCatalog> {
        val url = "${base()}/api/subscription/plans"
        if (base().isBlank()) return Result.failure(IllegalStateException("API not configured"))
        return try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val snippet = text.take(120).ifBlank { "HTTP ${response.code}" }
                    return Result.failure(Exception("Could not load plans ($snippet)"))
                }
                Result.success(parseCatalog(JSONObject(text)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createOrder(planId: String, teacherName: String, teacherPhone: String, idToken: String?): Result<CheckoutOrder> {
        val url = "${base()}/api/subscription/create-order"
        val body = JSONObject().apply {
            put("planId", planId)
            put("teacherName", teacherName)
            put("teacherPhone", teacherPhone)
        }
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!response.isSuccessful) {
                    val err = json?.optString("error") ?: text
                    return Result.failure(Exception(err.ifBlank { "Could not start payment" }))
                }
                Result.success(parseOrder(json ?: JSONObject()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        planId: String,
        idToken: String?,
    ): Result<PaymentSuccess> {
        val url = "${base()}/api/subscription/verify"
        val body = JSONObject().apply {
            put("orderId", orderId)
            put("paymentId", paymentId)
            put("signature", signature)
            put("planId", planId)
        }
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!idToken.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $idToken")

        return try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!response.isSuccessful) {
                    val err = json?.optString("error") ?: text
                    return Result.failure(Exception(err.ifBlank { "Payment verification failed" }))
                }
                val accountJson = json?.optJSONObject("account") ?: return Result.failure(Exception("No account"))
                Result.success(
                    PaymentSuccess(
                        paymentId = paymentId,
                        orderId = orderId,
                        signature = signature,
                        planId = planId,
                        account = AccountApiClient.parseAccount(accountJson),
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCatalog(json: JSONObject): SubscriptionCatalog {
        val marketingArr = json.optJSONArray("marketing") ?: JSONArray()
        val plansArr = json.optJSONArray("plans") ?: JSONArray()
        val comparisonArr = json.optJSONArray("comparison") ?: JSONArray()
        return SubscriptionCatalog(
            paymentsEnabled = json.optBoolean("paymentsEnabled", false),
            razorpayKeyId = json.optString("razorpayKeyId").takeIf { it.isNotBlank() },
            razorpayTestMode = json.optBoolean("razorpayTestMode", false),
            supportWhatsApp = json.optString("supportWhatsApp", "919876543210"),
            supportEmail = json.optString("supportEmail", "support@pedastudio.in"),
            marketing = (0 until marketingArr.length()).mapNotNull { i ->
                val o = marketingArr.optJSONObject(i) ?: return@mapNotNull null
                val tier = TierConfig.parseTier(o.optString("tier"))
                TierMarketing(
                    tier = tier,
                    label = o.optString("label", tier.label),
                    tagline = o.optString("tagline"),
                    highlights = o.optJSONArray("highlights")?.toStringList().orEmpty(),
                    badge = o.optString("badge").takeIf { it.isNotBlank() },
                )
            },
            comparison = (0 until comparisonArr.length()).mapNotNull { i ->
                val o = comparisonArr.optJSONObject(i) ?: return@mapNotNull null
                TierComparisonRow(
                    feature = o.optString("feature"),
                    basic = o.optString("basic"),
                    prime = o.optString("prime"),
                    max = o.optString("max"),
                )
            },
            plans = (0 until plansArr.length()).mapNotNull { i ->
                val o = plansArr.optJSONObject(i) ?: return@mapNotNull null
                PaidPlanOffer(
                    id = o.optString("id"),
                    tier = TierConfig.parseTier(o.optString("tier")),
                    billingCycle = o.optString("billingCycle"),
                    amountInr = o.optInt("amountInr"),
                    amountPaise = o.optInt("amountPaise"),
                    periodLabel = o.optString("periodLabel"),
                    savingsInr = if (o.has("savingsInr") && !o.isNull("savingsInr")) o.optInt("savingsInr") else null,
                    monthlyEquivalentInr = if (o.has("monthlyEquivalentInr") && !o.isNull("monthlyEquivalentInr")) {
                        o.optInt("monthlyEquivalentInr")
                    } else null,
                )
            },
        )
    }

    private fun parseOrder(json: JSONObject): CheckoutOrder {
        val plan = json.optJSONObject("plan")
        return CheckoutOrder(
            orderId = json.optString("orderId"),
            amountPaise = json.optInt("amountPaise"),
            amountInr = json.optInt("amountInr"),
            currency = json.optString("currency", "INR"),
            keyId = json.optString("keyId").takeIf { it.isNotBlank() },
            planId = plan?.optString("id") ?: json.optString("planId"),
            planLabel = plan?.optString("label") ?: "PedaStudio",
            prefillName = json.optJSONObject("prefill")?.optString("name").orEmpty(),
            prefillContact = json.optJSONObject("prefill")?.optString("contact").orEmpty(),
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        val out = mutableListOf<String>()
        for (i in 0 until length()) out.add(getString(i))
        return out
    }
}
