package com.product_catalog_offline_cart.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: Int,
    val title: String,
    val price: Double,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String,
    val quantity: Int,
    /** Epoch millis when first added; keeps the cart in the order items were added. */
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
