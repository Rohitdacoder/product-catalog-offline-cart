package com.product_catalog_offline_cart.ui.details

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.data.repository.CartRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.di.appContainer
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.ui.common.toErrorMessageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Success(val product: Product) : ProductDetailUiState
    data class Error(@param:StringRes val messageRes: Int) : ProductDetailUiState
}

class ProductDetailViewModel(
    private val repository: ProductRepository,
    private val cartRepository: CartRepository,
    private val productId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    // One-off snackbar messages: every tap produces its own message, delivered once.
    private val _cartMessages = Channel<Int>(Channel.BUFFERED)
    val cartMessages: Flow<Int> = _cartMessages.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadProduct()
    }

    fun retry() {
        loadProduct()
    }

    /** Stores the product in the local cart (Room only, no network); new → quantity 1, existing → +1. */
    fun addToCart(product: Product) {
        viewModelScope.launch {
            val message = try {
                cartRepository.addToCart(product)
                R.string.added_to_cart
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                R.string.add_to_cart_failed
            }
            _cartMessages.send(message)
        }
    }

    private fun loadProduct() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            _uiState.value = try {
                ProductDetailUiState.Success(repository.getProduct(productId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ProductDetailUiState.Error(e.toErrorMessageRes())
            }
        }
    }

    companion object {
        fun factory(productId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ProductDetailViewModel(
                    repository = container.productRepository,
                    cartRepository = container.cartRepository,
                    productId = productId,
                )
            }
        }
    }
}
