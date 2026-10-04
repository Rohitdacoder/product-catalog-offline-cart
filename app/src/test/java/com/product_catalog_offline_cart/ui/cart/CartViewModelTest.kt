package com.product_catalog_offline_cart.ui.cart

import com.product_catalog_offline_cart.data.repository.FakeCartRepository
import com.product_catalog_offline_cart.domain.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** uiState uses WhileSubscribed, so the test subscribes like the screen would. */
    private fun TestScope.createViewModel(repository: FakeCartRepository): CartViewModel =
        CartViewModel(repository).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }

    @Test
    fun `starts in Loading before the cart is read`() = runTest(dispatcher) {
        val viewModel = CartViewModel(FakeCartRepository(listOf(mascara)))

        assertEquals(CartUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `empty cart shows Empty`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeCartRepository())

        assertEquals(CartUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `observes cart items`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeCartRepository(listOf(mascara, iphone)))

        assertEquals(listOf(mascara, iphone), (viewModel.uiState.value as CartUiState.Content).items)
    }

    @Test
    fun `total items counts quantities not unique products`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeCartRepository(listOf(mascara.copy(quantity = 2), iphone.copy(quantity = 3))))

        assertEquals(5, (viewModel.uiState.value as CartUiState.Content).totalItems)
    }

    @Test
    fun `total price is the sum of line totals`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeCartRepository(listOf(mascara.copy(quantity = 2), iphone)))

        // 9.99 × 2 + 549.00 × 1
        assertEquals(568.98, (viewModel.uiState.value as CartUiState.Content).totalPrice, 0.0)
    }

    @Test
    fun `increase quantity calls repository and totals update`() = runTest(dispatcher) {
        val repository = FakeCartRepository(listOf(mascara))
        val viewModel = createViewModel(repository)

        viewModel.increaseQuantity(1)
        advanceUntilIdle()

        assertEquals(listOf("increase:1"), repository.calls)
        val state = viewModel.uiState.value as CartUiState.Content
        assertEquals(2, state.totalItems)
        assertEquals(19.98, state.totalPrice, 0.0)
    }

    @Test
    fun `decrease quantity calls repository`() = runTest(dispatcher) {
        val repository = FakeCartRepository(listOf(mascara.copy(quantity = 3)))
        val viewModel = createViewModel(repository)

        viewModel.decreaseQuantity(1)
        advanceUntilIdle()

        assertEquals(listOf("decrease:1"), repository.calls)
        assertEquals(2, (viewModel.uiState.value as CartUiState.Content).totalItems)
    }

    @Test
    fun `decreasing the last unit empties the cart`() = runTest(dispatcher) {
        val repository = FakeCartRepository(listOf(mascara))
        val viewModel = createViewModel(repository)

        viewModel.decreaseQuantity(1)
        advanceUntilIdle()

        assertEquals(CartUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `remove calls repository`() = runTest(dispatcher) {
        val repository = FakeCartRepository(listOf(mascara, iphone))
        val viewModel = createViewModel(repository)

        viewModel.removeFromCart(7)
        advanceUntilIdle()

        assertEquals(listOf("remove:7"), repository.calls)
        assertEquals(listOf(mascara), (viewModel.uiState.value as CartUiState.Content).items)
    }

    @Test
    fun `clear cart calls repository and shows Empty`() = runTest(dispatcher) {
        val repository = FakeCartRepository(listOf(mascara, iphone))
        val viewModel = createViewModel(repository)

        viewModel.clearCart()
        advanceUntilIdle()

        assertEquals(listOf("clear"), repository.calls)
        assertEquals(CartUiState.Empty, viewModel.uiState.value)
    }

    private companion object {
        val mascara = CartItem(productId = 1, title = "Mascara", price = 9.99, thumbnailUrl = "", quantity = 1)
        val iphone = CartItem(productId = 7, title = "iPhone 9", price = 549.0, thumbnailUrl = "", quantity = 1)
    }
}
