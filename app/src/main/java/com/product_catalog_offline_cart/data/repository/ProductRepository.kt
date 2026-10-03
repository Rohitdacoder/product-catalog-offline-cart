package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.domain.model.Product

/**
 * Source of product catalog data.
 *
 * Failures are not caught here: network problems surface as [java.io.IOException],
 * non-2xx responses as [retrofit2.HttpException] and malformed payloads as
 * [kotlinx.serialization.SerializationException]. Callers (ViewModels) decide how to present them.
 */
interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun searchProducts(query: String): List<Product>
    suspend fun getProduct(id: Int): Product
}
