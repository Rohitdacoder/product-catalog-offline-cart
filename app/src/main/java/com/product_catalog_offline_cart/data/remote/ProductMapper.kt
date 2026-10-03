package com.product_catalog_offline_cart.data.remote

import com.product_catalog_offline_cart.domain.model.Product

fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    category = category,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    brand = brand?.takeIf { it.isNotBlank() },
    thumbnailUrl = thumbnail,
    imageUrls = images,
)
