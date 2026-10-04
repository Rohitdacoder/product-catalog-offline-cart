package com.product_catalog_offline_cart.ui.common

import androidx.annotation.StringRes
import com.product_catalog_offline_cart.R
import retrofit2.HttpException
import java.io.IOException

/** Maps a repository failure to a user-facing message, shared by all screens. */
@StringRes
fun Throwable.toErrorMessageRes(): Int = when (this) {
    is IOException -> R.string.error_network
    is HttpException -> R.string.error_server
    else -> R.string.error_unknown
}
