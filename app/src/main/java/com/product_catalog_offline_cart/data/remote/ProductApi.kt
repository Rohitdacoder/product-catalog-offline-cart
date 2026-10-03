package com.product_catalog_offline_cart.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("products")
    suspend fun getProducts(): ProductResponseDto

    @GET("products/search")
    suspend fun searchProducts(@Query("q") query: String): ProductResponseDto

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto
}
