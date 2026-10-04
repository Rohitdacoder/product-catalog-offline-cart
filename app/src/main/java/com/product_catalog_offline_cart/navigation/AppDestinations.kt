package com.product_catalog_offline_cart.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. Only IDs are passed; screens load their own data. */
@Serializable
data object ProductListDestination

@Serializable
data class ProductDetailDestination(val productId: Int)
