package com.product_catalog_offline_cart.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CartTotalsTest {

    private fun item(id: Int, price: Double, quantity: Int) =
        CartItem(productId = id, title = "Item $id", price = price, thumbnailUrl = "", quantity = quantity)

    @Test
    fun `empty cart has zero totals`() {
        assertEquals(0, emptyList<CartItem>().totalItems())
        assertEquals(0.0, emptyList<CartItem>().totalPrice(), 0.0)
    }

    @Test
    fun `total items is the sum of quantities`() {
        val cart = listOf(item(1, 9.99, 2), item(2, 19.99, 1), item(3, 5.0, 4))

        assertEquals(7, cart.totalItems())
    }

    @Test
    fun `total price is the sum of price times quantity`() {
        val cart = listOf(item(1, 9.99, 2), item(2, 19.99, 1), item(3, 5.0, 4))

        // 19.98 + 19.99 + 20.00
        assertEquals(59.97, cart.totalPrice(), 0.0)
    }

    @Test
    fun `total price has no floating point drift`() {
        // Plain Double math gives 0.30000000000000004 for 0.1 × 3.
        val cart = listOf(item(1, 0.1, 3), item(2, 0.2, 1))

        assertEquals(0.5, cart.totalPrice(), 0.0)
    }

    @Test
    fun `line total is price times quantity`() {
        assertEquals(29.97, item(1, 9.99, 3).lineTotal, 0.0)
    }
}
