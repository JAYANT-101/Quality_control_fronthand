package com.example.myapplication.repository

import com.example.myapplication.data.CheckerOutputErrorResponse
import com.example.myapplication.data.CheckerOutputRequest
import com.example.myapplication.data.CheckerOutputResponse
import com.example.myapplication.network.CheckerOutputApiService
import kotlinx.serialization.json.Json

class CheckerOutputRepository(
    private val apiService: CheckerOutputApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun submitCheckerOutput(
        request: CheckerOutputRequest
    ): Result<CheckerOutputResponse> {
        return try {
            val response = apiService.submitCheckerOutput(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = parseErrors(errorBody) ?: "Request failed (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network failure: ${e.message}"))
        }
    }

    private fun parseErrors(jsonStr: String?): String? {
        if (jsonStr == null) return null
        return runCatching {
            json.decodeFromString<CheckerOutputErrorResponse>(jsonStr).errors.firstOrNull()
        }.getOrNull()
    }
}
