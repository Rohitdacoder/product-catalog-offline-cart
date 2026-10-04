package com.product_catalog_offline_cart.ui.common

import java.util.Locale

// DummyJSON prices are in USD.
fun formatPrice(price: Double): String = String.format(Locale.US, "$%.2f", price)
