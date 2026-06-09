package com.example.myapplication.repository

import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.network.ApiService
import kotlinx.serialization.json.Json
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun authorize(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.authorize(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.authorized) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body?.errors?.firstOrNull() ?: "Unknown error"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = errorBody?.let {
                    try {
                        json.decodeFromString<LoginResponse>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorResponse?.errors?.firstOrNull() ?: when (response.code()) {
                    401 -> "Invalid username or password"
                    400 -> "Validation error"
                    500 -> "Server error"
                    else -> "Network error: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }
}
