package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductTypesResponse(
    @SerialName("product_types") val productTypes: List<String>
)

@Serializable
data class PoNumbersResponse(
    @SerialName("product_type") val productType: String,
    @SerialName("po_numbers") val poNumbers: List<PoNumberItem>
)

@Serializable
data class PoNumberItem(
    @SerialName("po_id") val poId: Int,
    @SerialName("po_number") val poNumber: String,
    @SerialName("target") val target: Int
)

@Serializable
data class ApiErrorResponse(
    @SerialName("errors") val errors: List<String>
)
