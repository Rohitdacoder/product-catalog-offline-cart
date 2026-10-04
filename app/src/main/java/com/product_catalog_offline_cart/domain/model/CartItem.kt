package com.product_catalog_offline_cart.domain.model

import java.math.BigDecimal

/**
 * A product in the cart. Holds everything the cart needs to display offline,
 * captured when the product was added (price is a snapshot, not refreshed from the API).
 */
data class CartItem(
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnailUrl: String,
    val quantity: Int,
) {
    val lineTotal: Double get() = (price.toBigDecimal() * quantity.toBigDecimal()).toDouble()
}

fun Product.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    title = title,
    price = price,
    thumbnailUrl = thumbnailUrl,
    quantity = quantity,
)

/** Number of units in the cart, e.g. 2 × A + 1 × B = 3. */
fun List<CartItem>.totalItems(): Int = sumOf { it.quantity }

/** Sum of price × quantity. Uses BigDecimal so many lines don't accumulate floating-point error. */
fun List<CartItem>.totalPrice(): Double =
    fold(BigDecimal.ZERO) { total, item -> total + item.price.toBigDecimal() * item.quantity.toBigDecimal() }
        .toDouble()
