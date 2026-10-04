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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(val products: List<Product>) : ProductListUiState
    /** No products to show. [query] is the search that produced no matches, or blank for the full list. */
    data class Empty(val query: String) : ProductListUiState
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

    /** Trimmed query of the pending or in-flight request; null when idle. */
    private var activeQuery: String? = null

    /** Trimmed query whose results are currently on screen; null while loading or after an error. */
    private var displayedQuery: String? = null

    init {
        load(query = "", debounceMillis = 0)
    }

    /** Retries whatever the search field currently holds: the search if non-blank, otherwise the full list. */
    fun retry() {
        load(query = _searchQuery.value.trim(), debounceMillis = 0)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()
        // e.g. a trailing space: a request for this exact query is already pending or running.
        if (trimmed == activeQuery) return
        if (trimmed == displayedQuery) {
            // e.g. typing then deleting a character: the results on screen already match.
            loadJob?.cancel()
            activeQuery = null
            return
        }
        // Clearing the field is deliberate, so show the full list straight away.
        load(query = trimmed, debounceMillis = if (trimmed.isEmpty()) 0 else SEARCH_DEBOUNCE_MILLIS)
    }

    /**
     * Cancels any pending or in-flight request, so only the latest query can update [uiState].
     * Cancelling also aborts the underlying Retrofit call.
     */
    private fun load(query: String, debounceMillis: Long) {
        loadJob?.cancel()
        activeQuery = query
        loadJob = viewModelScope.launch {
            delay(debounceMillis)
            displayedQuery = null
            _uiState.value = ProductListUiState.Loading
            val newState = try {
                val products = if (query.isEmpty()) repository.getProducts() else repository.searchProducts(query)
                if (products.isEmpty()) ProductListUiState.Empty(query) else ProductListUiState.Success(products)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ProductListUiState.Error(e.toMessageRes())
            }
            // Never let a superseded request overwrite the UI.
            ensureActive()
            if (newState !is ProductListUiState.Error) displayedQuery = query
            activeQuery = null
            _uiState.value = newState
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L

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
