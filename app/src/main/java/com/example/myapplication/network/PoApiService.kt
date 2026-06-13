package com.example.myapplication.network

import com.example.myapplication.data.PoNumbersResponse
import com.example.myapplication.data.ProductTypesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PoApiService {
    @GET("api/po/product-types")
    suspend fun getProductTypes(): Response<ProductTypesResponse>

    @GET("api/po/po-numbers")
    suspend fun getPoNumbers(
        @Query("product_type") productType: String
    ): Response<PoNumbersResponse>
}
