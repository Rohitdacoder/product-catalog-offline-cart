package com.product_catalog_offline_cart

import android.app.Application
import com.product_catalog_offline_cart.di.AppContainer

class ProductCatalogApplication : Application() {

    /** Created once per process; see [AppContainer]. */
    val container: AppContainer by lazy { AppContainer(this) }
}
