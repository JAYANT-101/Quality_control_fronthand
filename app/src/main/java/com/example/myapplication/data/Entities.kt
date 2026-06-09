package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val po_number: String,
    val cloth_type: String,
    val color: String,
    val target: Int,
    val is_completed: Boolean = false
)

@Serializable
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task_id: String,
    val line_no: Int,
    val result: String,
    val defect_type: String?,
    val checker_id: Int
)
