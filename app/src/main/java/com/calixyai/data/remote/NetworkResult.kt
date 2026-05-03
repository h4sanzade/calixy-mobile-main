package com.calixyai.data.remote

import com.calixyai.data.remote.model.ErrorResponse
import com.google.gson.Gson
import retrofit2.Response

/**
 * Sealed hierarchy representing every possible outcome of a network call.
 * ViewModels never deal with raw Retrofit responses — only NetworkResult.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
}

/**
 * Safely executes a suspend Retrofit call and maps it to NetworkResult.
 * Parses the backend error body to extract the human-readable message.
 */
suspend fun <T> safeApiCall(
    gson: Gson,
    call: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("Empty response from server", response.code())
            }
        } else {
            val errorMessage = parseErrorBody(gson, response)
            NetworkResult.Error(errorMessage, response.code())
        }
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("No internet connection. Please check your network.")
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Request timed out. Please try again.")
    } catch (e: Exception) {
        NetworkResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
    }
}

private fun <T> parseErrorBody(gson: Gson, response: Response<T>): String {
    return try {
        val errorJson = response.errorBody()?.string()
        if (!errorJson.isNullOrBlank()) {
            val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)
            errorResponse.message
                ?: errorResponse.error
                ?: "Error ${response.code()}"
        } else {
            "Error ${response.code()}"
        }
    } catch (e: Exception) {
        "Error ${response.code()}"
    }
}