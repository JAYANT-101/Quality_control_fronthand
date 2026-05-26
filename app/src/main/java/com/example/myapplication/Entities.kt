package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val po_number: String,
    val cloth_type: String,
    val color: String,
    val target: Int,
    val is_completed: Boolean = false
)

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task_id: String,
    val line_no: Int,
    val result: String,
    val defect_type: String?,
    @ColumnInfo(name = "is_synced") val is_synced: Int = 0
)
