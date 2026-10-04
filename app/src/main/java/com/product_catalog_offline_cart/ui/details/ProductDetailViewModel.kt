package com.product_catalog_offline_cart.ui.details

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.product_catalog_offline_cart.data.remote.NetworkClient
import com.product_catalog_offline_cart.data.repository.DefaultProductRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.ui.common.toErrorMessageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Success(val product: Product) : ProductDetailUiState
    data class Error(@param:StringRes val messageRes: Int) : ProductDetailUiState
}

class ProductDetailViewModel(
    private val repository: ProductRepository,
    private val productId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadProduct()
    }

    fun retry() {
        loadProduct()
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
                ProductDetailViewModel(DefaultProductRepository(NetworkClient.productApi), productId)
            }
        }
    }
}
