package com.product_catalog_offline_cart.ui.details

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.data.repository.FakeCartRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeProductRepository()
    private val cartRepository = FakeCartRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Records every state the ViewModel emits, so tests can check an Error never appeared. */
    private fun TestScope.recordStates(viewModel: ProductDetailViewModel): List<ProductDetailUiState> {
        val states = mutableListOf<ProductDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(states) }
        return states
    }

    @Test
    fun `starts in Loading`() = runTest(dispatcher) {
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)

        assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loads the product for the given id`() = runTest(dispatcher) {
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)

        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Success(sampleProduct), viewModel.uiState.value)
        assertEquals(listOf(PRODUCT_ID), repository.requestedIds)
    }

    @Test
    fun `network failure shows network error`() = runTest(dispatcher) {
        repository.result = { throw IOException("offline") }

        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Error(R.string.error_network), viewModel.uiState.value)
    }

    @Test
    fun `http failure shows server error`() = runTest(dispatcher) {
        repository.result = { throw HttpException(Response.error<Any>(404, "".toResponseBody())) }

        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Error(R.string.error_server), viewModel.uiState.value)
    }

    @Test
    fun `unexpected failure shows generic error`() = runTest(dispatcher) {
        repository.result = { throw IllegalStateException("bad payload") }

        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Error(R.string.error_unknown), viewModel.uiState.value)
    }

    @Test
    fun `retry after failure loads the product`() = runTest(dispatcher) {
        repository.result = { throw IOException("offline") }
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        repository.result = { sampleProduct }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Success(sampleProduct), viewModel.uiState.value)
        assertEquals(listOf(PRODUCT_ID, PRODUCT_ID), repository.requestedIds)
    }

    @Test
    fun `retry shows Loading while the request is in flight`() = runTest(dispatcher) {
        repository.result = { throw IOException("offline") }
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        val pending = CompletableDeferred<Product>()
        repository.result = { pending.await() }
        viewModel.retry()
        runCurrent()
        assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)

        pending.complete(sampleProduct)
        advanceUntilIdle()
        assertEquals(ProductDetailUiState.Success(sampleProduct), viewModel.uiState.value)
    }

    @Test
    fun `clearing the ViewModel mid-request does not produce an error`() = runTest(dispatcher) {
        var requestCancelled = false
        repository.result = {
            try {
                CompletableDeferred<Product>().await()
            } catch (e: CancellationException) {
                requestCancelled = true
                throw e
            }
        }
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(
            store,
            viewModelFactory { initializer { ProductDetailViewModel(repository, cartRepository, PRODUCT_ID) } },
        )[ProductDetailViewModel::class]
        val states = recordStates(viewModel)
        runCurrent()

        // Same as leaving the screen: viewModelScope is cancelled.
        store.clear()
        advanceUntilIdle()

        assertTrue(requestCancelled)
        assertEquals(listOf(ProductDetailUiState.Loading), states)
    }

    @Test
    fun `retry during an in-flight request cancels it without an error`() = runTest(dispatcher) {
        val first = CompletableDeferred<Product>()
        var firstCancelled = false
        var calls = 0
        repository.result = {
            if (++calls == 1) {
                try {
                    first.await()
                } catch (e: CancellationException) {
                    firstCancelled = true
                    throw e
                }
            } else {
                sampleProduct
            }
        }
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        val states = recordStates(viewModel)
        runCurrent()

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(firstCancelled)
        assertFalse(states.any { it is ProductDetailUiState.Error })
        assertEquals(ProductDetailUiState.Success(sampleProduct), viewModel.uiState.value)
    }

    @Test
    fun `add to cart stores the product and confirms`() = runTest(dispatcher) {
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        viewModel.addToCart(sampleProduct)
        advanceUntilIdle()

        assertEquals(listOf("add:$PRODUCT_ID"), cartRepository.calls)
        assertEquals(1, cartRepository.items.value.single().quantity)
        assertEquals(R.string.added_to_cart, viewModel.cartMessages.first())
    }

    @Test
    fun `adding the same product twice increases quantity and confirms each time`() = runTest(dispatcher) {
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        val messages = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.cartMessages.toList(messages) }
        advanceUntilIdle()

        viewModel.addToCart(sampleProduct)
        viewModel.addToCart(sampleProduct)
        advanceUntilIdle()

        assertEquals(2, cartRepository.items.value.single().quantity)
        assertEquals(listOf(R.string.added_to_cart, R.string.added_to_cart), messages)
    }

    @Test
    fun `add to cart failure shows failure message`() = runTest(dispatcher) {
        cartRepository.addToCartError = IllegalStateException("disk full")
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        viewModel.addToCart(sampleProduct)
        advanceUntilIdle()

        assertTrue(cartRepository.items.value.isEmpty())
        assertEquals(R.string.add_to_cart_failed, viewModel.cartMessages.first())
    }

    @Test
    fun `add to cart does not reload the product`() = runTest(dispatcher) {
        val viewModel = ProductDetailViewModel(repository, cartRepository, PRODUCT_ID)
        advanceUntilIdle()

        viewModel.addToCart(sampleProduct)
        advanceUntilIdle()

        assertEquals(listOf(PRODUCT_ID), repository.requestedIds)
        assertEquals(ProductDetailUiState.Success(sampleProduct), viewModel.uiState.value)
    }

    private class FakeProductRepository : ProductRepository {
        var result: suspend () -> Product = { sampleProduct }
        val requestedIds = mutableListOf<Int>()

        override suspend fun getProduct(id: Int): Product {
            requestedIds += id
            return result()
        }

        override suspend fun getProducts(): List<Product> = error("Not used by product details")
        override suspend fun searchProducts(query: String): List<Product> = error("Not used by product details")
    }

    private companion object {
        const val PRODUCT_ID = 7

        val sampleProduct = Product(
            id = PRODUCT_ID, title = "iPhone 9", description = "An apple mobile", category = "smartphones",
            price = 549.0, discountPercentage = 12.96, rating = 4.69, stock = 94, brand = "Apple",
            thumbnailUrl = "", imageUrls = emptyList(),
        )
    }
}
