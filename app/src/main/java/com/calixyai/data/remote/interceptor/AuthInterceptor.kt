package com.calixyai.data.remote.interceptor

import com.calixyai.data.local.TokenStore
import com.calixyai.data.remote.model.RefreshTokenRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intercepts every request to:
 *   1. Attach Authorization header automatically.
 *   2. On 401 → attempt token refresh, retry original request once.
 *   3. If refresh fails → clear tokens (triggers logout flow via TokenStore state).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val gson: Gson
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val REFRESH_ENDPOINT = "api/auth/refresh"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip token injection for the refresh endpoint itself to avoid loops
        if (originalRequest.url.encodedPath.contains(REFRESH_ENDPOINT)) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenStore.getAccessToken()
        val authenticatedRequest = originalRequest.withBearerToken(accessToken)
        val response = chain.proceed(authenticatedRequest)

        if (response.code == 401) {
            response.close()
            return handleUnauthorized(chain, originalRequest)
        }

        return response
    }

    private fun handleUnauthorized(
        chain: Interceptor.Chain,
        originalRequest: Request
    ): Response {
        val refreshToken = tokenStore.getRefreshToken()

        if (refreshToken.isNullOrBlank()) {
            tokenStore.clearTokens()
            // Return a synthetic 401 so the UI can react
            return chain.proceed(originalRequest)
        }

        val refreshed = runBlocking { refreshTokens(chain, refreshToken) }

        return if (refreshed != null) {
            tokenStore.saveTokens(refreshed.first, refreshed.second)
            val retryRequest = originalRequest.withBearerToken(refreshed.first)
            chain.proceed(retryRequest)
        } else {
            tokenStore.clearTokens()
            chain.proceed(originalRequest)
        }
    }

    /**
     * Performs a synchronous refresh token call using a raw OkHttp call
     * (not Retrofit) to avoid circular dependency with the main client.
     */
    private fun refreshTokens(
        chain: Interceptor.Chain,
        refreshToken: String
    ): Pair<String, String>? {
        return try {
            val baseUrl = chain.request().url.run { "${scheme}://${host}/" }
            val json = gson.toJson(RefreshTokenRequest(refreshToken))
            val body = json.toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url("${baseUrl}${REFRESH_ENDPOINT}")
                .post(body)
                .build()

            val plainClient = OkHttpClient()
            val response = plainClient.newCall(refreshRequest).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return null
                val authResponse = gson.fromJson(
                    responseBody,
                    com.calixyai.data.remote.model.AuthResponse::class.java
                )
                Pair(authResponse.accessToken, authResponse.refreshToken)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun Request.withBearerToken(token: String?): Request {
        return if (!token.isNullOrBlank()) {
            newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            this
        }
    }
}