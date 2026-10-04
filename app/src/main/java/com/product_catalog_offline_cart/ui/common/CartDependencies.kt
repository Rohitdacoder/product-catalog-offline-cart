package com.product_catalog_offline_cart.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.product_catalog_offline_cart.data.local.AppDatabase
import com.product_catalog_offline_cart.data.repository.CartRepository
import com.product_catalog_offline_cart.data.repository.DefaultCartRepository

/**
 * Builds the Room-backed [CartRepository] inside a ViewModel factory.
 * All screens share the single [AppDatabase] instance; the repository itself is stateless.
 */
fun CreationExtras.cartRepository(): CartRepository {
    val application = checkNotNull(this[APPLICATION_KEY]) { "Application is required to open the cart database" }
    return DefaultCartRepository(AppDatabase.getInstance(application).cartDao())
}
