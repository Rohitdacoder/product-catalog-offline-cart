package com.product_catalog_offline_cart.ui.products

import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in Loading then shows products on success`() = runTest(dispatcher) {
        val viewModel = ProductListViewModel(FakeProductRepository { listOf(sampleProduct) })

        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Success(listOf(sampleProduct)), viewModel.uiState.value)
    }

    @Test
    fun `empty result shows Empty`() = runTest(dispatcher) {
        val viewModel = ProductListViewModel(FakeProductRepository { emptyList() })

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `network failure shows network error`() = runTest(dispatcher) {
        val viewModel = ProductListViewModel(FakeProductRepository { throw IOException("offline") })

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Error(R.string.error_network), viewModel.uiState.value)
    }

    @Test
    fun `http failure shows server error`() = runTest(dispatcher) {
        val viewModel = ProductListViewModel(
            FakeProductRepository { throw HttpException(Response.error<Any>(500, "".toResponseBody())) },
        )

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Error(R.string.error_server), viewModel.uiState.value)
    }

    @Test
    fun `retry after failure loads products`() = runTest(dispatcher) {
        var fail = true
        val viewModel = ProductListViewModel(
            FakeProductRepository { if (fail) throw IOException("offline") else listOf(sampleProduct) },
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Error(R.string.error_network), viewModel.uiState.value)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Success(listOf(sampleProduct)), viewModel.uiState.value)
    }

    @Test
    fun `retry shows Loading while request is in flight`() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<Product>>()
        var firstCall = true
        val viewModel = ProductListViewModel(
            FakeProductRepository {
                if (firstCall) {
                    firstCall = false
                    throw IOException("offline")
                }
                pending.await()
            },
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.retry()
        dispatcher.scheduler.runCurrent()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        pending.complete(listOf(sampleProduct))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProductListUiState.Success(listOf(sampleProduct)), viewModel.uiState.value)
    }

    @Test
    fun `search query updates without reloading`() = runTest(dispatcher) {
        var calls = 0
        val viewModel = ProductListViewModel(FakeProductRepository { calls++; listOf(sampleProduct) })
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("phone")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("phone", viewModel.searchQuery.value)
        assertEquals(1, calls)
    }

    private class FakeProductRepository(
        private val products: suspend () -> List<Product>,
    ) : ProductRepository {
        override suspend fun getProducts(): List<Product> = products()
        override suspend fun searchProducts(query: String): List<Product> = error("Not used yet")
        override suspend fun getProduct(id: Int): Product = error("Not used yet")
    }

    private companion object {
        val sampleProduct = Product(
            id = 1, title = "Mascara", description = "", category = "beauty",
            price = 9.99, discountPercentage = 0.0, rating = 4.5, stock = 10, brand = null,
            thumbnailUrl = "", imageUrls = emptyList(),
        )
    }
}
