package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * The shopping cart, stored locally only. No operation touches the network,
 * so the cart works offline and survives app restarts.
 */
interface CartRepository {

    /** Emits the cart now and again after every change, in the order items were added. */
    fun observeCart(): Flow<List<CartItem>>

    /** Adds [product] with quantity 1, or increases its quantity by 1 if it's already in the cart. */
    suspend fun addToCart(product: Product)

    /** Increases quantity by 1. Does nothing if the product isn't in the cart. */
    suspend fun increaseQuantity(productId: Int)

    /** Decreases quantity by 1; when quantity is 1 the item is removed instead. Never reaches 0 or below. */
    suspend fun decreaseQuantity(productId: Int)

    suspend fun removeFromCart(productId: Int)

    suspend fun clearCart()
}
