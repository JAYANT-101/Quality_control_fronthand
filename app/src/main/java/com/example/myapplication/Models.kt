package com.example.myapplication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    @SerialName("po_number") val poNumber: String,
    @SerialName("cloth_type") val clothType: String,
    val color: String,
    val target: Int
)

@Serializable
data class InspectionLog(
    @SerialName("task_id") val taskId: String,
    @SerialName("line_no") val lineNo: Int,
    val result: String,
    @SerialName("defect_type") val defectType: String? = null,
    val timestamp: Long
)
