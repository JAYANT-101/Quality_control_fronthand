package com.example.myapplication.repository

import com.example.myapplication.data.AuthResponse
import com.example.myapplication.data.AuthResult
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.network.AuthApiClient
import kotlinx.serialization.json.Json
import retrofit2.Response

class AuthRepository(private val authApiClient: AuthApiClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun login(usernameInput: String, passwordInput: String): AuthResult {
        return try {
            val response = authApiClient.authorize(LoginRequest(usernameInput, passwordInput))
            handleAuthResponse(response)
        } catch (e: Exception) {
            AuthResult(isSuccess = false, message = "Connection failed: ${e.message}")
        }
    }

    suspend fun verifySession(): AuthResult {
        return try {
            val response = authApiClient.getSession()
            handleAuthResponse(response)
        } catch (e: Exception) {
            AuthResult(isSuccess = false, message = "Connection failed: ${e.message}")
        }
    }

    suspend fun logout(): AuthResult {
        return try {
            val response = authApiClient.logout()
            if (response.isSuccessful) {
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, message = "Logout failed")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, message = "Connection failed: ${e.message}")
        }
    }

    private fun handleAuthResponse(response: Response<AuthResponse>): AuthResult {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.authorized && body.data != null) {
                AuthResult(isSuccess = true, user = body.data)
            } else {
                AuthResult(isSuccess = false, message = body?.errors?.firstOrNull() ?: "Session invalid")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorResponse = errorBody?.let {
                try {
                    json.decodeFromString<AuthResponse>(it)
                } catch (e: Exception) {
                    null
                }
            }
            val errorMessage = errorResponse?.errors?.firstOrNull() ?: "Session expired or invalid"
            AuthResult(isSuccess = false, message = errorMessage)
        }
    }
}
