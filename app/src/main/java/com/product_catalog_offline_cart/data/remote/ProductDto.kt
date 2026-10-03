package com.product_catalog_offline_cart.data.remote

import kotlinx.serialization.Serializable

/**
 * A single product as returned by DummyJSON. Only the fields the app needs are declared;
 * the rest are ignored by the [kotlinx.serialization.json.Json] configuration in [NetworkClient].
 */
@Serializable
data class ProductDto(
    val id: Int,
    val title: String,
    val description: String = "",
    val category: String = "",
    val price: Double,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    // Not every product has a brand (e.g. groceries).
    val brand: String? = null,
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
)
