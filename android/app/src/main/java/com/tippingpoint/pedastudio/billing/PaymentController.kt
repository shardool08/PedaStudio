package com.tippingpoint.pedastudio.billing

import android.app.Activity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.tippingpoint.pedastudio.api.CheckoutOrder
import org.json.JSONObject

/** Opens Razorpay checkout; MainActivity forwards [onPaymentSuccess] / [onPaymentError]. */
class RazorpayPaymentHandler(private val activity: Activity) {
    private var pending: PendingCheckout? = null
    private var preloaded = false

    data class PaymentResult(
        val paymentId: String,
        val orderId: String,
        val signature: String,
        val planId: String,
    )

    private fun ensurePreloaded() {
        if (preloaded) return
        preloaded = true
        runCatching { Checkout.preload(activity.applicationContext) }
    }

    fun startCheckout(order: CheckoutOrder, fallbackKeyId: String? = null, onResult: (Result<PaymentResult>) -> Unit) {
        ensurePreloaded()
        val keyId = order.keyId?.takeIf { it.isNotBlank() } ?: fallbackKeyId
        if (keyId.isNullOrBlank()) {
            onResult(Result.failure(IllegalStateException("Payment key not configured")))
            return
        }
        pending = PendingCheckout(order, onResult)

        val checkout = Checkout()
        checkout.setKeyID(keyId)
        val options = JSONObject().apply {
            put("name", "PedaStudio")
            put("description", order.planLabel)
            put("order_id", order.orderId)
            put("currency", order.currency)
            put("amount", order.amountPaise)
            put("prefill", JSONObject().apply {
                put("name", order.prefillName)
                if (order.prefillContact.isNotBlank()) put("contact", order.prefillContact)
            })
            put("theme", JSONObject().apply { put("color", "#2A7A6A") })
        }
        checkout.open(activity, options)
    }

    fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val state = pending ?: return
        pending = null
        if (razorpayPaymentId.isNullOrBlank() || paymentData == null) {
            state.callback(Result.failure(IllegalStateException("Payment incomplete")))
            return
        }
        val signature = paymentData.signature
        if (signature.isNullOrBlank()) {
            state.callback(Result.failure(IllegalStateException("Missing payment signature")))
            return
        }
        state.callback(
            Result.success(
                PaymentResult(
                    paymentId = razorpayPaymentId,
                    orderId = paymentData.orderId ?: state.order.orderId,
                    signature = signature,
                    planId = state.order.planId,
                ),
            ),
        )
    }

    fun onPaymentError(code: Int, response: String?) {
        val state = pending ?: return
        pending = null
        state.callback(Result.failure(Exception(response ?: "Payment cancelled (code $code)")))
    }

    private data class PendingCheckout(
        val order: CheckoutOrder,
        val callback: (Result<PaymentResult>) -> Unit,
    )
}
