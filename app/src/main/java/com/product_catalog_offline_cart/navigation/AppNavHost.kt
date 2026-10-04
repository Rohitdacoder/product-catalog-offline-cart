package com.product_catalog_offline_cart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.product_catalog_offline_cart.ui.details.ProductDetailRoute
import com.product_catalog_offline_cart.ui.details.ProductDetailViewModel
import com.product_catalog_offline_cart.ui.products.ProductListRoute

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ProductListDestination,
        modifier = modifier,
    ) {
        // The list stays on the back stack, so its ViewModel (search query, results) survives a visit to details.
        composable<ProductListDestination> { backStackEntry ->
            ProductListRoute(
                onProductClick = { productId ->
                    // Ignore extra taps while the transition to details is already running.
                    if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(ProductDetailDestination(productId))
                    }
                },
            )
        }

        composable<ProductDetailDestination> { backStackEntry ->
            val productId = backStackEntry.toRoute<ProductDetailDestination>().productId
            ProductDetailRoute(
                viewModel = viewModel(factory = ProductDetailViewModel.factory(productId)),
                // navigateUp() never pops the start destination, so a double tap can't leave a blank screen.
                onBack = { navController.navigateUp() },
                // Cart persistence is added in the cart step; nothing is stored yet.
                onAddToCart = { },
            )
        }
    }
}
