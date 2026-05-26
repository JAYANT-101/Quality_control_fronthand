package com.example.myapplication

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/tasks/active")
    suspend fun getActiveTasks(): List<Task>

    @POST("api/submit_inspection")
    suspend fun submitInspection(@Body log: InspectionLog): retrofit2.Response<Unit>
}
