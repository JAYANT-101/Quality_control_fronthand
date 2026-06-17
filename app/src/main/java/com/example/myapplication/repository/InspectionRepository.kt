package com.example.myapplication.repository

import com.example.myapplication.data.ApiDao
import com.example.myapplication.data.InspectionEntity
import kotlinx.coroutines.flow.Flow

class InspectionRepository(
    private val apiDao: ApiDao
) {
    fun getCount(poNumber: String, lineNo: Int, result: String): Flow<Int> {
        return apiDao.getInspectionCountFlow(poNumber, lineNo, result)
    }

    suspend fun saveInspection(inspection: InspectionEntity) {
        apiDao.insertInspection(inspection)
    }

    suspend fun resetAllCountsAndTasks() {
        apiDao.deleteAllInspections()
    }

    suspend fun clearCountsForPo(poNumber: String, lineNo: Int) {
        apiDao.deleteInspectionsByPo(poNumber, lineNo)
    }
}
