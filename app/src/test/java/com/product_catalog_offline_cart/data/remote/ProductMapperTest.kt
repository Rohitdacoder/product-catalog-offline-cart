package com.product_catalog_offline_cart.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductMapperTest {

    @Test
    fun `parses DummyJSON payload ignoring unknown fields and maps to domain`() {
        val payload = """
            {
              "products": [
                {
                  "id": 1,
                  "title": "Essence Mascara Lash Princess",
                  "description": "Volumizing mascara",
                  "category": "beauty",
                  "price": 9.99,
                  "discountPercentage": 10.48,
                  "rating": 2.56,
                  "stock": 99,
                  "tags": ["beauty", "mascara"],
                  "brand": "Essence",
                  "dimensions": { "width": 15.14, "height": 13.08, "depth": 22.99 },
                  "images": ["https://cdn.dummyjson.com/1.webp"],
                  "thumbnail": "https://cdn.dummyjson.com/thumbnail.webp"
                },
                {
                  "id": 16,
                  "title": "Apple",
                  "price": 1.99,
                  "thumbnail": "https://cdn.dummyjson.com/apple.webp"
                }
              ],
              "total": 194,
              "skip": 0,
              "limit": 2
            }
        """.trimIndent()

        val response = NetworkClient.json.decodeFromString<ProductResponseDto>(payload)
        val products = response.products.map { it.toDomain() }

        assertEquals(194, response.total)
        assertEquals(2, products.size)

        with(products[0]) {
            assertEquals(1, id)
            assertEquals("Essence", brand)
            assertEquals(9.99, price, 0.0)
            assertEquals("https://cdn.dummyjson.com/thumbnail.webp", thumbnailUrl)
            assertEquals(listOf("https://cdn.dummyjson.com/1.webp"), imageUrls)
        }

        with(products[1]) {
            assertNull(brand)
            assertEquals("", description)
            assertEquals(emptyList<String>(), imageUrls)
        }
    }
}
