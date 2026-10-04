package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.data.local.CartDao
import com.product_catalog_offline_cart.data.local.toDomain
import com.product_catalog_offline_cart.data.local.toEntity
import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.domain.model.toCartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Depends only on the local DAO: there is deliberately no ProductApi here.
// Room runs suspend DAO calls on its own background executor, so these functions are main-safe.
class DefaultCartRepository(
    private val cartDao: CartDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : CartRepository {

    override fun observeCart(): Flow<List<CartItem>> =
        cartDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addToCart(product: Product) {
        val inserted = cartDao.insertIfAbsent(product.toCartItem(quantity = 1).toEntity(currentTimeMillis()))
        if (inserted == ALREADY_IN_CART) {
            cartDao.incrementQuantity(product.id)
        }
    }

    override suspend fun increaseQuantity(productId: Int) {
        cartDao.incrementQuantity(productId)
    }

    override suspend fun decreaseQuantity(productId: Int) {
        val decremented = cartDao.decrementQuantityAboveOne(productId)
        if (decremented == 0) {
            // Quantity was 1 (or the item is already gone): remove rather than going to 0.
            cartDao.delete(productId)
        }
    }

    override suspend fun removeFromCart(productId: Int) {
        cartDao.delete(productId)
    }

    override suspend fun clearCart() {
        cartDao.clear()
    }

    private companion object {
        /** Value Room returns from an IGNORE insert when the primary key already exists. */
        const val ALREADY_IN_CART = -1L
    }
}
