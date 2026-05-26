package com.example.myapplication

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.apiDao()
        val apiService = RetrofitClient.apiService

        val unsyncedLogs = dao.getUnsyncedLogs()

        if (unsyncedLogs.isEmpty()) return Result.success()

        var allSuccessful = true

        unsyncedLogs.forEach { entity ->
            try {
                val log = InspectionLog(
                    taskId = entity.task_id,
                    lineNo = entity.line_no,
                    result = entity.result,
                    defectType = entity.defect_type,
                    timestamp = System.currentTimeMillis() / 1000 // Unix Seconds
                )

                val response = apiService.submitInspection(log)
                if (response.isSuccessful) {
                    dao.markAsSynced(entity.id)
                } else {
                    allSuccessful = false
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        return if (allSuccessful) Result.success() else Result.retry()
    }
}
