package com.example.myapplication.repository

import com.example.myapplication.data.ApiDao
import com.example.myapplication.data.InspectionEntity
import com.example.myapplication.data.TaskEntity
import com.example.myapplication.network.ApiService
import kotlinx.coroutines.flow.Flow

class InspectionRepository(
    private val apiDao: ApiDao,
    private val apiService: ApiService
) {
    val allTasks: Flow<List<TaskEntity>> = apiDao.getAllTasksFlow()

    suspend fun refreshTasks() {
        try {
            val tasks = apiService.getTasks()
            apiDao.insertTasks(tasks)
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun getCount(poNumber: String, lineNo: Int, result: String): Flow<Int> {
        return apiDao.getInspectionCountFlow(poNumber, lineNo, result)
    }

    suspend fun saveInspection(inspection: InspectionEntity) {
        apiDao.insertInspection(inspection)
        try {
            apiService.uploadInspection(inspection)
        } catch (e: Exception) {
            // Logic for offline sync could go here
        }
    }

    suspend fun markTaskAsCompleted(poNumber: String) {
        apiDao.markTaskAsCompleted(poNumber)
    }

    suspend fun resetAllCountsAndTasks() {
        apiDao.resetAllTasks()
        apiDao.deleteAllInspections()
    }
}
