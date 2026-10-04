package com.product_catalog_offline_cart.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.product_catalog_offline_cart.data.repository.DefaultCartRepository
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room SQL against an in-memory SQLite database on a device/emulator. */
@RunWith(AndroidJUnit4::class)
class CartDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CartDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.cartDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(id: Int, quantity: Int = 1, addedAt: Long = id.toLong()) = CartEntity(
        productId = id, title = "Item $id", price = 9.99, thumbnailUrl = "thumb-$id", quantity = quantity, addedAt = addedAt,
    )

    @Test
    fun insertIfAbsent_ignoresDuplicateProduct() = runBlocking {
        assertTrue(dao.insertIfAbsent(entity(1)) != -1L)
        assertEquals(-1L, dao.insertIfAbsent(entity(1, quantity = 5)))

        assertEquals(listOf(entity(1)), dao.observeAll().first())
    }

    @Test
    fun observeAll_ordersByTimeAdded() = runBlocking {
        dao.insertIfAbsent(entity(3, addedAt = 10))
        dao.insertIfAbsent(entity(1, addedAt = 20))

        assertEquals(listOf(3, 1), dao.observeAll().first().map { it.productId })
    }

    @Test
    fun incrementQuantity_updatesOnlyExistingRow() = runBlocking {
        dao.insertIfAbsent(entity(1))

        assertEquals(1, dao.incrementQuantity(1))
        assertEquals(0, dao.incrementQuantity(99))
        assertEquals(2, dao.observeAll().first().single().quantity)
    }

    @Test
    fun decrementQuantityAboveOne_neverGoesBelowOne() = runBlocking {
        dao.insertIfAbsent(entity(1, quantity = 2))

        assertEquals(1, dao.decrementQuantityAboveOne(1))
        assertEquals(0, dao.decrementQuantityAboveOne(1))
        assertEquals(1, dao.observeAll().first().single().quantity)
    }

    @Test
    fun deleteAndClear_removeRows() = runBlocking {
        dao.insertIfAbsent(entity(1))
        dao.insertIfAbsent(entity(2))

        assertEquals(1, dao.delete(1))
        assertEquals(listOf(2), dao.observeAll().first().map { it.productId })

        dao.clear()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun repository_fullFlowAgainstRealDatabase() = runBlocking {
        val repository = DefaultCartRepository(dao)
        val product = Product(
            id = 7, title = "iPhone 9", description = "", category = "smartphones", price = 549.0,
            discountPercentage = 0.0, rating = 4.69, stock = 94, brand = "Apple", thumbnailUrl = "thumb",
            imageUrls = emptyList(),
        )

        repository.addToCart(product)
        repository.addToCart(product)
        assertEquals(2, repository.observeCart().first().single().quantity)

        repository.decreaseQuantity(7)
        repository.decreaseQuantity(7)
        assertTrue(repository.observeCart().first().isEmpty())
    }
}
