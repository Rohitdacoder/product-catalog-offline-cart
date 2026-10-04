package com.product_catalog_offline_cart.data.repository

import com.product_catalog_offline_cart.data.local.CartEntity
import com.product_catalog_offline_cart.data.local.FakeCartDao
import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCartRepositoryTest {

    private val dao = FakeCartDao()
    private var now = 1_000L
    private val repository = DefaultCartRepository(dao, currentTimeMillis = { now++ })

    private fun quantityOf(productId: Int): Int? = dao.items.find { it.productId == productId }?.quantity

    @Test
    fun `adding a new product stores it with quantity 1`() = runTest {
        repository.addToCart(iphone)

        assertEquals(
            listOf(
                CartEntity(
                    productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "iphone-thumb",
                    quantity = 1, addedAt = 1_000L,
                ),
            ),
            dao.items,
        )
    }

    @Test
    fun `adding the same product again increases its quantity`() = runTest {
        repository.addToCart(iphone)
        repository.addToCart(iphone)
        repository.addToCart(iphone)

        assertEquals(1, dao.items.size)
        assertEquals(3, quantityOf(7))
    }

    @Test
    fun `adding the same product again keeps its original position`() = runTest {
        repository.addToCart(iphone)
        repository.addToCart(mascara)
        repository.addToCart(iphone)

        assertEquals(listOf(7, 1), repository.observeCart().first().map { it.productId })
    }

    @Test
    fun `increase quantity adds one`() = runTest {
        repository.addToCart(iphone)

        repository.increaseQuantity(7)

        assertEquals(2, quantityOf(7))
    }

    @Test
    fun `increase quantity for a product not in the cart does nothing`() = runTest {
        repository.increaseQuantity(99)

        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `decrease quantity above 1 subtracts one`() = runTest {
        repository.addToCart(iphone)
        repository.increaseQuantity(7)
        repository.increaseQuantity(7)

        repository.decreaseQuantity(7)

        assertEquals(2, quantityOf(7))
    }

    @Test
    fun `decrease quantity from 1 removes the item`() = runTest {
        repository.addToCart(iphone)
        repository.addToCart(mascara)

        repository.decreaseQuantity(7)

        assertEquals(listOf(1), dao.items.map { it.productId })
    }

    @Test
    fun `quantity never becomes zero or negative`() = runTest {
        val seenQuantities = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dao.observeAll().collect { rows -> rows.forEach { seenQuantities += it.quantity } }
        }
        repository.addToCart(iphone)
        repository.increaseQuantity(7)

        repeat(5) { repository.decreaseQuantity(7) }

        assertTrue(dao.items.isEmpty())
        assertEquals(listOf(1, 2, 1), seenQuantities)
        assertTrue(seenQuantities.all { it > 0 })
    }

    @Test
    fun `decrease quantity for a product not in the cart does nothing`() = runTest {
        repository.addToCart(mascara)

        repository.decreaseQuantity(99)

        assertEquals(1, quantityOf(1))
    }

    @Test
    fun `remove deletes the item regardless of quantity`() = runTest {
        repository.addToCart(iphone)
        repository.increaseQuantity(7)
        repository.addToCart(mascara)

        repository.removeFromCart(7)

        assertEquals(listOf(1), dao.items.map { it.productId })
    }

    @Test
    fun `clear removes everything`() = runTest {
        repository.addToCart(iphone)
        repository.addToCart(mascara)

        repository.clearCart()

        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `observeCart emits domain items after every change`() = runTest {
        val emissions = mutableListOf<List<CartItem>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeCart().toList(emissions)
        }

        repository.addToCart(iphone)
        repository.addToCart(iphone)
        repository.removeFromCart(7)

        assertEquals(
            listOf(
                emptyList(),
                listOf(CartItem(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "iphone-thumb", quantity = 1)),
                listOf(CartItem(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "iphone-thumb", quantity = 2)),
                emptyList(),
            ),
            emissions,
        )
    }

    private companion object {
        val iphone = Product(
            id = 7, title = "iPhone 9", description = "", category = "smartphones",
            price = 549.0, discountPercentage = 0.0, rating = 4.69, stock = 94, brand = "Apple",
            thumbnailUrl = "iphone-thumb", imageUrls = emptyList(),
        )
        val mascara = Product(
            id = 1, title = "Mascara", description = "", category = "beauty",
            price = 9.99, discountPercentage = 0.0, rating = 2.56, stock = 99, brand = null,
            thumbnailUrl = "mascara-thumb", imageUrls = emptyList(),
        )
    }
}
