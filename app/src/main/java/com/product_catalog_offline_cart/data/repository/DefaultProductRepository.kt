package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.data.remote.ProductApi
import com.product_catalog_offline_cart.data.remote.toDomain
import com.product_catalog_offline_cart.domain.model.Product

// Retrofit suspend calls are main-safe, so no dispatcher switching is needed here.
class DefaultProductRepository(
    private val api: ProductApi,
) : ProductRepository {

    override suspend fun getProducts(): List<Product> =
        api.getProducts().products.map { it.toDomain() }

    override suspend fun searchProducts(query: String): List<Product> =
        api.searchProducts(query.trim()).products.map { it.toDomain() }

    override suspend fun getProduct(id: Int): Product =
        api.getProduct(id).toDomain()
}
