package com.example.myapplication.network

import com.example.myapplication.data.CheckerOutputRequest
import com.example.myapplication.data.CheckerOutputResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckerOutputApiService {
    @POST("checker-output")
    suspend fun submitCheckerOutput(
        @Body request: CheckerOutputRequest
    ): Response<CheckerOutputResponse>
}
