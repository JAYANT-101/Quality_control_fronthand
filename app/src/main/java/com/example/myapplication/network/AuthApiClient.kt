package com.example.myapplication.network

import com.example.myapplication.data.AuthResponse
import com.example.myapplication.data.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiClient {
    @POST("users/authorize")
    suspend fun authorize(@Body request: LoginRequest): Response<AuthResponse>

    @GET("users/session")
    suspend fun getSession(): Response<AuthResponse>

    @POST("users/logout")
    suspend fun logout(): Response<AuthResponse>
}
