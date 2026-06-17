package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(log: InspectionEntity)

    @Query("DELETE FROM inspections")
    suspend fun deleteAllInspections()

    @Query("DELETE FROM inspections WHERE task_id = :poNumber AND line_no = :lineNo")
    suspend fun deleteInspectionsByPo(poNumber: String, lineNo: Int)

    @Query("SELECT COUNT(*) FROM inspections WHERE task_id = :poNumber AND line_no = :lineNo AND result = :result")
    fun getInspectionCountFlow(poNumber: String, lineNo: Int, result: String): Flow<Int>
}
