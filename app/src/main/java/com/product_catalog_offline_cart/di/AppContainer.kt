package com.product_catalog_offline_cart.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.product_catalog_offline_cart.ProductCatalogApplication
import com.product_catalog_offline_cart.data.local.AppDatabase
import com.product_catalog_offline_cart.data.remote.NetworkClient
import com.product_catalog_offline_cart.data.repository.CartRepository
import com.product_catalog_offline_cart.data.repository.DefaultCartRepository
import com.product_catalog_offline_cart.data.repository.DefaultProductRepository
import com.product_catalog_offline_cart.data.repository.ProductRepository

/**
 * Manual dependency container: the one place where the app's long-lived objects are created.
 * Lives as long as the process (owned by [ProductCatalogApplication]), so every screen shares
 * the same HTTP client, database and repositories. Everything is lazy: nothing is opened until first use.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val networkClient by lazy { NetworkClient() }

    private val database by lazy { AppDatabase.getInstance(appContext) }

    val productRepository: ProductRepository by lazy {
        DefaultProductRepository(networkClient.productApi)
    }

    /** Room only; never touches [networkClient]. */
    val cartRepository: CartRepository by lazy {
        DefaultCartRepository(database.cartDao())
    }
}

/** Gives ViewModel factories access to the container through the [CreationExtras] they receive. */
fun CreationExtras.appContainer(): AppContainer {
    val application = checkNotNull(this[APPLICATION_KEY]) { "ViewModel factory needs the Application" }
    return (application as ProductCatalogApplication).container
}
