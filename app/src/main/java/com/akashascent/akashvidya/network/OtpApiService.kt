package com.akashascent.akashvidya.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OtpApiService {
    @POST("api/otp/send")
    suspend fun sendOtp(
        @Body request: OtpSendRequest
    ): Response<OtpResponse>

    @POST("api/otp/verify")
    suspend fun verifyOtp(
        @Body request: OtpVerifyRequest
    ): Response<OtpResponse>
}
