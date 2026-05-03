package com.calixyai.data.repository

import com.calixyai.data.local.TokenStore
import com.calixyai.data.remote.NetworkResult
import com.calixyai.data.remote.api.AuthApiService
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
import com.calixyai.data.remote.safeApiCall
import com.calixyai.domain.repository.AuthRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenStore: TokenStore,
    private val gson: Gson
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): NetworkResult<MessageResponse> =
        safeApiCall(gson) {
            api.register(RegisterRequest(email, password, confirmPassword))
        }

    override suspend fun verifyEmail(
        email: String,
        code: String
    ): NetworkResult<AuthResponse> {
        val result = safeApiCall(gson) {
            api.verifyEmail(VerifyEmailRequest(email, code))
        }
        if (result is NetworkResult.Success) {
            tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun login(
        email: String,
        password: String
    ): NetworkResult<AuthResponse> {
        val result = safeApiCall(gson) {
            api.login(LoginRequest(email, password))
        }
        if (result is NetworkResult.Success) {
            tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun googleLogin(idToken: String): NetworkResult<AuthResponse> {
        val result = safeApiCall(gson) {
            api.googleLogin(GoogleLoginRequest(idToken))
        }
        if (result is NetworkResult.Success) {
            tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun logout(): NetworkResult<MessageResponse> {
        val accessToken = tokenStore.getAccessToken().orEmpty()
        val refreshToken = tokenStore.getRefreshToken().orEmpty()

        val result = safeApiCall(gson) {
            api.logout("Bearer $accessToken", refreshToken)
        }
        // Always clear tokens regardless of API outcome
        tokenStore.clearTokens()
        return result
    }

    override suspend fun resendVerification(email: String): NetworkResult<MessageResponse> =
        safeApiCall(gson) {
            api.resendVerification(ResendVerificationRequest(email))
        }

    override suspend fun forgotPassword(email: String): NetworkResult<MessageResponse> =
        safeApiCall(gson) {
            api.forgotPassword(ForgotPasswordRequest(email))
        }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): NetworkResult<MessageResponse> =
        safeApiCall(gson) {
            api.resetPassword(ResetPasswordRequest(email, code, newPassword))
        }
}