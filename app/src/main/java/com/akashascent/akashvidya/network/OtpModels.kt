package com.akashascent.akashvidya.network

data class OtpSendRequest(
    val mobile: String,
    val otp: String
)

data class OtpVerifyRequest(
    val mobile: String,
    val otp: String
)

data class OtpResponse(
    val success: Boolean,
    val message: String,
    val data: OtpData? = null
)

data class OtpData(
    val requestId: String? = null
)
