package com.example.myapplication.repository

import com.example.myapplication.data.ApiErrorResponse
import com.example.myapplication.data.PoNumberItem
import com.example.myapplication.network.PoApiService
import com.example.myapplication.session.SessionManager
import kotlinx.serialization.json.Json

class PoRepository(
    private val poApiService: PoApiService,
    private val sessionManager: SessionManager,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchProductTypes(): Result<List<String>> {
        if (!sessionManager.verifySessionBeforeAction()) {
            return Result.failure(Exception("Session invalid"))
        }

        return try {
            val response = poApiService.getProductTypes()
            if (response.isSuccessful) {
                Result.success(response.body()?.productTypes ?: emptyList())
            } else {
                val errorBody = response.errorBody()?.string()
                val message = parseErrors(errorBody) ?: "Request failed (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network failure: ${e.message}"))
        }
    }

    suspend fun fetchPoNumbers(productType: String): Result<List<PoNumberItem>> {
        if (!sessionManager.verifySessionBeforeAction()) {
            return Result.failure(Exception("Session invalid"))
        }

        return try {
            val response = poApiService.getPoNumbers(productType)
            if (response.isSuccessful) {
                val body = response.body()
                println("PoRepository: Success! Received ${body?.poNumbers?.size} PO numbers for $productType")
                body?.poNumbers?.forEach { println("PoRepository: PO: ${it.poNumber}") }
                Result.success(body?.poNumbers ?: emptyList())
            } else {
                val errorBody = response.errorBody()?.string()
                println("PoRepository: Error response: $errorBody")
                val message = parseErrors(errorBody) ?: "Request failed (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            println("PoRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Network failure: ${e.message}"))
        }
    }

    private fun parseErrors(jsonStr: String?): String? {
        if (jsonStr == null) return null
        return runCatching {
            json.decodeFromString<ApiErrorResponse>(jsonStr).errors.firstOrNull()
        }.getOrNull()
    }
}
