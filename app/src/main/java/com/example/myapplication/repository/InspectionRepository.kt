package com.example.myapplication.repository

import com.example.myapplication.data.ApiDao
import com.example.myapplication.data.InspectionEntity
import com.example.myapplication.data.TaskEntity
import com.example.myapplication.session.SessionManager
import kotlinx.coroutines.flow.Flow

class InspectionRepository(
    private val apiDao: ApiDao,
    private val sessionManager: SessionManager
) {
    val allTasks: Flow<List<TaskEntity>> = apiDao.getAllTasksFlow()

    suspend fun refreshTasks() {
        if (!sessionManager.verifySessionBeforeAction()) return
        
        // Removed server task fetching as ApiService is deprecated
    }

    fun getCount(poNumber: String, lineNo: Int, result: String): Flow<Int> {
        return apiDao.getInspectionCountFlow(poNumber, lineNo, result)
    }

    suspend fun saveInspection(inspection: InspectionEntity) {
        apiDao.insertInspection(inspection)
        
        if (!sessionManager.verifySessionBeforeAction()) return

        // Removed server upload as ApiService is deprecated
    }

    suspend fun markTaskAsCompleted(poNumber: String) {
        apiDao.markTaskAsCompleted(poNumber)
    }

    suspend fun resetAllCountsAndTasks() {
        apiDao.resetAllTasks()
        apiDao.deleteAllInspections()
    }
}
