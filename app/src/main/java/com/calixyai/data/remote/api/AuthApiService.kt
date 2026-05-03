package com.calixyai.data.remote.api

import com.calixyai.data.remote.model.AuthResponse
import com.calixyai.data.remote.model.ForgotPasswordRequest
import com.calixyai.data.remote.model.GoogleLoginRequest
import com.calixyai.data.remote.model.LoginRequest
import com.calixyai.data.remote.model.MessageResponse
import com.calixyai.data.remote.model.RefreshTokenRequest
import com.calixyai.data.remote.model.RegisterRequest
import com.calixyai.data.remote.model.ResendVerificationRequest
import com.calixyai.data.remote.model.ResetPasswordRequest
import com.calixyai.data.remote.model.VerifyEmailRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<MessageResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String,
        @Header("Refresh-Token") refreshToken: String
    ): Response<MessageResponse>

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): Response<MessageResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<MessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<MessageResponse>
}