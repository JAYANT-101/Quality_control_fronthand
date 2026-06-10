package com.example.myapplication.network

import com.example.myapplication.data.TaskEntity
import com.example.myapplication.data.InspectionEntity
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @GET("tasks")
    suspend fun getTasks(): List<TaskEntity>

    @POST("inspections")
    suspend fun uploadInspection(@Body inspection: InspectionEntity)
}
