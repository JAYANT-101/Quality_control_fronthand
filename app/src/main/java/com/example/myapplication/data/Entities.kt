package com.example.myapplication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerialName("task_id") @ColumnInfo(name = "task_id") val taskId: String,
    @SerialName("line_no") @ColumnInfo(name = "line_no") val lineNo: Int,
    @SerialName("result") val result: String,
    @SerialName("defect_type") @ColumnInfo(name = "defect_type") val defectType: String?,
    @SerialName("checker_id") @ColumnInfo(name = "checker_id") val checkerId: Int,
)
