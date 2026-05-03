package com.calixyai.domain.repository

import com.calixyai.data.remote.NetworkResult
import com.calixyai.data.remote.model.AuthResponse
import com.calixyai.data.remote.model.MessageResponse

/**
 * Contract for authentication operations.
 * The domain layer depends on this abstraction, not the concrete implementation.
 */
interface AuthRepository {

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): NetworkResult<MessageResponse>

    suspend fun verifyEmail(
        email: String,
        code: String
    ): NetworkResult<AuthResponse>

    suspend fun login(
        email: String,
        password: String
    ): NetworkResult<AuthResponse>

    suspend fun googleLogin(
        idToken: String
    ): NetworkResult<AuthResponse>

    suspend fun logout(): NetworkResult<MessageResponse>

    suspend fun resendVerification(email: String): NetworkResult<MessageResponse>

    suspend fun forgotPassword(email: String): NetworkResult<MessageResponse>

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): NetworkResult<MessageResponse>
}