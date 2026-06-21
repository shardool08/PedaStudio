package com.tippingpoint.pedastudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.tippingpoint.pedastudio.auth.PhoneAuthController
import com.tippingpoint.pedastudio.billing.RazorpayPaymentHandler
import com.tippingpoint.pedastudio.ui.theme.PedaStudioTheme

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val phoneAuth = PhoneAuthController()
    lateinit var paymentHandler: RazorpayPaymentHandler
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentHandler = RazorpayPaymentHandler(this)
        enableEdgeToEdge()
        setContent {
            PedaStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PedaStudioApp(auth = phoneAuth, paymentHandler = paymentHandler)
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentHandler.onPaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        paymentHandler.onPaymentError(code, response)
    }
}
