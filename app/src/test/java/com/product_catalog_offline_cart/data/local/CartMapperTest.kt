package com.product_catalog_offline_cart.data.local

import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.domain.model.toCartItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CartMapperTest {

    private val product = Product(
        id = 7, title = "iPhone 9", description = "An apple mobile", category = "smartphones",
        price = 549.0, discountPercentage = 12.96, rating = 4.69, stock = 94, brand = "Apple",
        thumbnailUrl = "https://cdn.dummyjson.com/iphone/thumbnail.webp",
        imageUrls = listOf("https://cdn.dummyjson.com/iphone/1.webp"),
    )

    @Test
    fun `product maps to cart item with quantity 1 by default`() {
        assertEquals(
            CartItem(
                productId = 7,
                title = "iPhone 9",
                price = 549.0,
                thumbnailUrl = "https://cdn.dummyjson.com/iphone/thumbnail.webp",
                quantity = 1,
            ),
            product.toCartItem(),
        )
    }

    @Test
    fun `cart item maps to entity with structured columns`() {
        val item = CartItem(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "thumb", quantity = 3)

        assertEquals(
            CartEntity(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "thumb", quantity = 3, addedAt = 1_000L),
            item.toEntity(addedAt = 1_000L),
        )
    }

    @Test
    fun `entity maps back to cart item`() {
        val entity = CartEntity(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "thumb", quantity = 3, addedAt = 1_000L)

        assertEquals(
            CartItem(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "thumb", quantity = 3),
            entity.toDomain(),
        )
    }

    @Test
    fun `cart item survives a round trip through the entity`() {
        val item = product.toCartItem(quantity = 2)

        assertEquals(item, item.toEntity(addedAt = 5L).toDomain())
    }
}
