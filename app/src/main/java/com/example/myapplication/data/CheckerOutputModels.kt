package com.example.myapplication.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class CheckerOutputRequest(
    @SerialName("user_id") val userId: Int,
    @SerialName("line") val line: Int,
    @SerialName("po_id") val poId: Int,
    @SerialName("field_name") val fieldName: String,
    @SerialName("defect_name") val defectName: String,
    @SerialName("actual_event_time") val actualEventTime: String,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class CheckerOutputResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data") val data: CheckerOutputData,
    @SerialName("po") val po: PoProgress,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class CheckerOutputData(
    @SerialName("user_id") val userId: Int,
    @SerialName("line") val line: Int,
    @SerialName("po_id") val poId: Int,
    @SerialName("field_name") val fieldName: String,
    @SerialName("defect_name") val defectName: String? = null,
    @SerialName("actual_event_time") val actualEventTime: String,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class PoProgress(
    @SerialName("po_id") val poId: Int,
    @SerialName("target") val target: Int,
    @SerialName("produced") val produced: Int,
    @SerialName("remaining_target") val remainingTarget: Int,
    @SerialName("completed") val completed: Boolean,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class CheckerOutputErrorResponse(
    @SerialName("status") val status: String,
    @SerialName("errors") val errors: List<String>,
)
