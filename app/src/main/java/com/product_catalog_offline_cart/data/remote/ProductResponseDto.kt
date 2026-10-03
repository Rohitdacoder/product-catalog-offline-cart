package com.product_catalog_offline_cart.data.remote

import kotlinx.serialization.Serializable

/** Paged envelope returned by `/products` and `/products/search`. */
@Serializable
data class ProductResponseDto(
    val products: List<ProductDto>,
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0,
)
