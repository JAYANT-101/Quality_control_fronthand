package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckerOutputRequest(
    @SerialName("user_id") val userId: Int,
    @SerialName("line") val line: Int,
    @SerialName("po_id") val poId: Int,
    @SerialName("field_name") val fieldName: String,
    @SerialName("defect_name") val defectName: String,
    @SerialName("actual_event_time") val actualEventTime: String
)

@Serializable
data class CheckerOutputResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data") val data: CheckerOutputData
)

@Serializable
data class CheckerOutputData(
    @SerialName("user_id") val userId: Int,
    @SerialName("line") val line: Int,
    @SerialName("po_id") val poId: Int,
    @SerialName("field_name") val fieldName: String,
    @SerialName("defect_name") val defectName: String? = null,
    @SerialName("actual_event_time") val actualEventTime: String
)

@Serializable
data class CheckerOutputErrorResponse(
    @SerialName("status") val status: String,
    @SerialName("errors") val errors: List<String>
)

enum class CheckerFieldName(val value: String) {
    PASS("pass"),
    REJECT("reject"),
    ALTER("alter")
}
