package com.akashascent.akashvidya.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApiService {
    @POST("api/payment/verify")
    suspend fun verifyPayment(
        @Body request: PaymentVerificationRequest
    ): Response<PaymentResponse>
}

data class PaymentVerificationRequest(
    val razorpay_payment_id: String,
    val razorpay_order_id: String? = null,
    val razorpay_signature: String? = null,
    val userId: String,
    val playlistId: String,
    val amount: String,
    val itemName: String
)

data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val invoiceUrl: String? = null
)
