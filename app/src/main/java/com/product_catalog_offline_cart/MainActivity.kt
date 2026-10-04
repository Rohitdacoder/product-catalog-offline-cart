package com.product_catalog_offline_cart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.product_catalog_offline_cart.navigation.AppNavHost
import com.product_catalog_offline_cart.ui.theme.ProductcatalogofflinecartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProductcatalogofflinecartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Insets are consumed here so nested Scaffolds (e.g. details) don't pad for system bars twice.
                    AppNavHost(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                    )
                }
            }
        }
    }
}
