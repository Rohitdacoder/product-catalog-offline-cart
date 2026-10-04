package com.product_catalog_offline_cart.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CartDao] mirroring the SQL in the real DAO, for JVM unit tests. */
class FakeCartDao : CartDao {

    private val rows = MutableStateFlow<List<CartEntity>>(emptyList())

    val items: List<CartEntity> get() = rows.value

    override fun observeAll(): Flow<List<CartEntity>> = rows

    override suspend fun insertIfAbsent(item: CartEntity): Long {
        if (rows.value.any { it.productId == item.productId }) return -1L
        rows.update { current -> (current + item).sortedBy { it.addedAt } }
        return item.productId.toLong()
    }

    override suspend fun incrementQuantity(productId: Int): Int =
        updateWhere({ it.productId == productId }) { it.copy(quantity = it.quantity + 1) }

    override suspend fun decrementQuantityAboveOne(productId: Int): Int =
        updateWhere({ it.productId == productId && it.quantity > 1 }) { it.copy(quantity = it.quantity - 1) }

    override suspend fun delete(productId: Int): Int {
        val before = rows.value.size
        rows.update { current -> current.filterNot { it.productId == productId } }
        return before - rows.value.size
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    private fun updateWhere(predicate: (CartEntity) -> Boolean, transform: (CartEntity) -> CartEntity): Int {
        val matches = rows.value.count(predicate)
        rows.update { current -> current.map { if (predicate(it)) transform(it) else it } }
        return matches
    }
}
