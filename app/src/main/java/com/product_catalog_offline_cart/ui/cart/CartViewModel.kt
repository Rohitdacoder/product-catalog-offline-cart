package com.product_catalog_offline_cart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.product_catalog_offline_cart.data.repository.CartRepository
import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.domain.model.totalItems
import com.product_catalog_offline_cart.domain.model.totalPrice
import com.product_catalog_offline_cart.ui.common.cartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CartUiState {
    /** Before Room's first emission (very brief). */
    data object Loading : CartUiState
    data object Empty : CartUiState
    data class Content(
        val items: List<CartItem>,
        val totalItems: Int,
        val totalPrice: Double,
    ) : CartUiState
}

/** Reads and changes the cart through [CartRepository] only (Room), so every action works offline. */
class CartViewModel(
    private val cartRepository: CartRepository,
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = cartRepository.observeCart()
        .map { items ->
            if (items.isEmpty()) {
                CartUiState.Empty
            } else {
                // Totals are derived from the current items every time, never stored.
                CartUiState.Content(items = items, totalItems = items.totalItems(), totalPrice = items.totalPrice())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState.Loading)

    fun increaseQuantity(productId: Int) {
        viewModelScope.launch { cartRepository.increaseQuantity(productId) }
    }

    /** At quantity 1 the repository removes the item, so quantity never reaches 0. */
    fun decreaseQuantity(productId: Int) {
        viewModelScope.launch { cartRepository.decreaseQuantity(productId) }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch { cartRepository.removeFromCart(productId) }
    }

    fun clearCart() {
        viewModelScope.launch { cartRepository.clearCart() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CartViewModel(cartRepository()) }
        }
    }
}
