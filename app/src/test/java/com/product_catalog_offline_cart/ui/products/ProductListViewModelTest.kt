package com.product_catalog_offline_cart.ui.products

import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.data.repository.FakeCartRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.ui.products.ProductListViewModel.Companion.SEARCH_DEBOUNCE_MILLIS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

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

    private fun TestScope.createLoadedViewModel(): ProductListViewModel =
        ProductListViewModel(repository, cartRepository).also { advanceUntilIdle() }

    /** Types [query] and waits until the debounced request has finished. */
    private fun TestScope.search(viewModel: ProductListViewModel, query: String) {
        viewModel.onSearchQueryChange(query)
        advanceUntilIdle()
    }

    // region Initial product list

    @Test
    fun `blank query loads products on start`() = runTest(dispatcher) {
        val viewModel = ProductListViewModel(repository, cartRepository)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)
        assertEquals(1, repository.getProductsCalls)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `empty product list shows Empty without a query`() = runTest(dispatcher) {
        repository.products = { emptyList() }

        val viewModel = createLoadedViewModel()

        assertEquals(ProductListUiState.Empty(""), viewModel.uiState.value)
    }

    @Test
    fun `network failure shows network error`() = runTest(dispatcher) {
        repository.products = { throw IOException("offline") }

        val viewModel = createLoadedViewModel()

        assertEquals(ProductListUiState.Error(R.string.error_network), viewModel.uiState.value)
    }

    @Test
    fun `http failure shows server error`() = runTest(dispatcher) {
        repository.products = { throw HttpException(Response.error<Any>(500, "".toResponseBody())) }

        val viewModel = createLoadedViewModel()

        assertEquals(ProductListUiState.Error(R.string.error_server), viewModel.uiState.value)
    }

    @Test
    fun `retry with blank query reloads the product list`() = runTest(dispatcher) {
        repository.products = { throw IOException("offline") }
        val viewModel = createLoadedViewModel()

        repository.products = { allProducts }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)
        assertEquals(2, repository.getProductsCalls)
        assertTrue(repository.searchCalls.isEmpty())
    }

    // endregion

    // region Search

    @Test
    fun `non-blank query calls searchProducts and shows results`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        search(viewModel, "phone")

        assertEquals(listOf("phone"), repository.searchCalls)
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
        assertEquals(1, repository.getProductsCalls)
    }

    @Test
    fun `search shows Loading while the request is in flight`() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<Product>>()
        repository.search = { pending.await() }
        val viewModel = createLoadedViewModel()

        viewModel.onSearchQueryChange("phone")
        // Previous results stay visible during the debounce window.
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        pending.complete(phoneResults)
        advanceUntilIdle()
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `search query is trimmed before searching`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        search(viewModel, "  phone ")

        assertEquals("  phone ", viewModel.searchQuery.value)
        assertEquals(listOf("phone"), repository.searchCalls)
    }

    @Test
    fun `empty search result shows Empty with the query`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        search(viewModel, "zzz")

        assertEquals(ProductListUiState.Empty("zzz"), viewModel.uiState.value)
    }

    @Test
    fun `search error shows Error state`() = runTest(dispatcher) {
        repository.search = { throw IOException("offline") }
        val viewModel = createLoadedViewModel()

        search(viewModel, "phone")

        assertEquals(ProductListUiState.Error(R.string.error_network), viewModel.uiState.value)
    }

    @Test
    fun `retry while searching retries the current query`() = runTest(dispatcher) {
        repository.search = { throw IOException("offline") }
        val viewModel = createLoadedViewModel()
        search(viewModel, "phone")

        repository.search = ::defaultSearch
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf("phone", "phone"), repository.searchCalls)
        assertEquals(1, repository.getProductsCalls)
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `retry is not debounced`() = runTest(dispatcher) {
        repository.search = { throw IOException("offline") }
        val viewModel = createLoadedViewModel()
        search(viewModel, "phone")

        viewModel.retry()
        runCurrent()

        assertEquals(listOf("phone", "phone"), repository.searchCalls)
    }

    @Test
    fun `clearing search returns to the product list immediately`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        search(viewModel, "phone")

        viewModel.onSearchQueryChange("")
        runCurrent()

        assertEquals(2, repository.getProductsCalls)
        advanceUntilIdle()
        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)
    }

    @Test
    fun `changing the query cancels the in-flight search and ignores its result`() = runTest(dispatcher) {
        val slowResult = CompletableDeferred<List<Product>>()
        var staleSearchCancelled = false
        repository.search = { query ->
            if (query == "pho") {
                try {
                    slowResult.await()
                } catch (e: CancellationException) {
                    staleSearchCancelled = true
                    throw e
                }
            } else {
                defaultSearch(query)
            }
        }
        val viewModel = createLoadedViewModel()

        // "pho" passes the debounce and its request starts.
        viewModel.onSearchQueryChange("pho")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        assertEquals(listOf("pho"), repository.searchCalls)

        viewModel.onSearchQueryChange("phone")
        advanceUntilIdle()
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
        assertTrue(staleSearchCancelled)

        // Even if the stale response arrived now, it must not replace the latest results.
        slowResult.complete(listOf(sampleProduct(99, "Stale")))
        advanceUntilIdle()
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `slow stale search finishing after a newer one does not overwrite it`() = runTest(dispatcher) {
        repository.search = { query ->
            if (query == "pho") delay(5_000)
            defaultSearch(query)
        }
        val viewModel = createLoadedViewModel()

        viewModel.onSearchQueryChange("pho")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        viewModel.onSearchQueryChange("phone")
        advanceUntilIdle()

        assertEquals(listOf("pho", "phone"), repository.searchCalls)
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `debounce sends only one request for fast typing`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        listOf("p", "ph", "pho", "phon", "phone").forEach { query ->
            viewModel.onSearchQueryChange(query)
            advanceTimeBy(100)
        }
        // 100ms after the last keystroke: still inside the debounce window.
        assertTrue(repository.searchCalls.isEmpty())
        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)

        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        runCurrent()
        assertEquals(listOf("phone"), repository.searchCalls)
    }

    @Test
    fun `no request before the debounce delay has passed`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        viewModel.onSearchQueryChange("phone")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        runCurrent()

        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `whitespace-only change does not trigger a new request`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        search(viewModel, "phone")

        search(viewModel, "phone ")

        assertEquals(listOf("phone"), repository.searchCalls)
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `typing then reverting to the shown query does not refetch`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        search(viewModel, "phone")

        viewModel.onSearchQueryChange("phones")
        advanceTimeBy(100)
        search(viewModel, "phone")

        assertEquals(listOf("phone"), repository.searchCalls)
        assertEquals(ProductListUiState.Success(phoneResults), viewModel.uiState.value)
    }

    @Test
    fun `whitespace-only query is treated as blank`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()

        search(viewModel, "   ")

        assertTrue(repository.searchCalls.isEmpty())
        assertEquals(1, repository.getProductsCalls)
        assertEquals(ProductListUiState.Success(allProducts), viewModel.uiState.value)
    }

    // endregion

    // region Cart badge

    @Test
    fun `cart badge counts total quantity not unique products`() = runTest(dispatcher) {
        cartRepository.items.value = listOf(
            CartItem(productId = 1, title = "Mascara", price = 9.99, thumbnailUrl = "", quantity = 2),
            CartItem(productId = 2, title = "iPhone 9", price = 549.0, thumbnailUrl = "", quantity = 3),
        )
        val viewModel = createLoadedViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.cartItemCount.collect {} }
        advanceUntilIdle()

        assertEquals(5, viewModel.cartItemCount.value)
    }

    @Test
    fun `cart badge is zero for an empty cart and follows changes`() = runTest(dispatcher) {
        val viewModel = createLoadedViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.cartItemCount.collect {} }
        advanceUntilIdle()
        assertEquals(0, viewModel.cartItemCount.value)

        cartRepository.items.value = listOf(
            CartItem(productId = 1, title = "Mascara", price = 9.99, thumbnailUrl = "", quantity = 4),
        )
        advanceUntilIdle()

        assertEquals(4, viewModel.cartItemCount.value)
    }

    // endregion

    private class FakeProductRepository : ProductRepository {
        var products: suspend () -> List<Product> = { allProducts }
        var search: suspend (String) -> List<Product> = ::defaultSearch

        var getProductsCalls = 0
            private set
        val searchCalls = mutableListOf<String>()

        override suspend fun getProducts(): List<Product> {
            getProductsCalls++
            return products()
        }

        override suspend fun searchProducts(query: String): List<Product> {
            searchCalls += query
            return search(query)
        }

        override suspend fun getProduct(id: Int): Product = error("Not used by the product list")
    }

    private companion object {
        val allProducts = listOf(sampleProduct(1, "Mascara"), sampleProduct(2, "iPhone 9"))
        val phoneResults = listOf(sampleProduct(2, "iPhone 9"))

        fun defaultSearch(query: String): List<Product> =
            allProducts.filter { it.title.contains(query, ignoreCase = true) }

        fun sampleProduct(id: Int, title: String) = Product(
            id = id, title = title, description = "", category = "",
            price = 9.99, discountPercentage = 0.0, rating = 4.5, stock = 10, brand = null,
            thumbnailUrl = "", imageUrls = emptyList(),
        )
    }
}
