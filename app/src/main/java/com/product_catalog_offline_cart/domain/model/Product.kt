package com.product_catalog_offline_cart.domain.model

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String?,
    val thumbnailUrl: String,
    val imageUrls: List<String>,
)
