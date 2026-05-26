package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    fun getAllTasksFlow(): kotlinx.coroutines.flow.Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(log: InspectionEntity)

    @Query("SELECT * FROM inspections WHERE is_synced = 0")
    suspend fun getUnsyncedLogs(): List<InspectionEntity>

    @Query("UPDATE inspections SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
    @Query("UPDATE tasks SET is_completed = 1 WHERE po_number = :poNumber")
    suspend fun markTaskAsCompleted(poNumber: String)
}
