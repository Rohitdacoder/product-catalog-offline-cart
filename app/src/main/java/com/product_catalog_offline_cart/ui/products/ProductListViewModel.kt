package com.product_catalog_offline_cart.ui.products

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.data.remote.NetworkClient
import com.product_catalog_offline_cart.data.repository.DefaultProductRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository
import com.product_catalog_offline_cart.domain.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(val products: List<Product>) : ProductListUiState
    data object Empty : ProductListUiState
    data class Error(@param:StringRes val messageRes: Int) : ProductListUiState
}

class ProductListViewModel(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadProducts()
    }

    fun retry() {
        loadProducts()
    }

    /** Only updates the query text for now; filtering/API search is wired up in the search step. */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadProducts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading
            _uiState.value = try {
                val products = repository.getProducts()
                if (products.isEmpty()) ProductListUiState.Empty else ProductListUiState.Success(products)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ProductListUiState.Error(e.toMessageRes())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProductListViewModel(DefaultProductRepository(NetworkClient.productApi))
            }
        }
    }
}

@StringRes
private fun Exception.toMessageRes(): Int = when (this) {
    is IOException -> R.string.error_network
    is HttpException -> R.string.error_server
    else -> R.string.error_unknown
}
