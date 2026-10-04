package com.product_catalog_offline_cart.data.local

import com.product_catalog_offline_cart.domain.model.CartItem

fun CartEntity.toDomain(): CartItem = CartItem(
    productId = productId,
    title = title,
    price = price,
    thumbnailUrl = thumbnailUrl,
    quantity = quantity,
)

fun CartItem.toEntity(addedAt: Long): CartEntity = CartEntity(
    productId = productId,
    title = title,
    price = price,
    thumbnailUrl = thumbnailUrl,
    quantity = quantity,
    addedAt = addedAt,
)
