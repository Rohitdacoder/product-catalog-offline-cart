package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.domain.model.toCartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CartRepository] for ViewModel tests. Records calls and follows the real quantity rules. */
class FakeCartRepository(initialItems: List<CartItem> = emptyList()) : CartRepository {

    val items = MutableStateFlow(initialItems)
    val calls = mutableListOf<String>()

    /** Set to make [addToCart] fail, e.g. to simulate a disk error. */
    var addToCartError: Exception? = null

    override fun observeCart(): Flow<List<CartItem>> = items

    override suspend fun addToCart(product: Product) {
        calls += "add:${product.id}"
        addToCartError?.let { throw it }
        items.update { current ->
            if (current.any { it.productId == product.id }) {
                current.map { if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                current + product.toCartItem()
            }
        }
    }

    override suspend fun increaseQuantity(productId: Int) {
        calls += "increase:$productId"
        items.update { current -> current.map { if (it.productId == productId) it.copy(quantity = it.quantity + 1) else it } }
    }

    override suspend fun decreaseQuantity(productId: Int) {
        calls += "decrease:$productId"
        items.update { current ->
            current.mapNotNull {
                when {
                    it.productId != productId -> it
                    it.quantity > 1 -> it.copy(quantity = it.quantity - 1)
                    else -> null
                }
            }
        }
    }

    override suspend fun removeFromCart(productId: Int) {
        calls += "remove:$productId"
        items.update { current -> current.filterNot { it.productId == productId } }
    }

    override suspend fun clearCart() {
        calls += "clear"
        items.value = emptyList()
    }
}
