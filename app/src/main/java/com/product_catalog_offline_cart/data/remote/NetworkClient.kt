package com.product_catalog_offline_cart.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the HTTP stack for the DummyJSON API. Created once by
 * [com.product_catalog_offline_cart.di.AppContainer]; [baseUrl] can be swapped, e.g. for a test server.
 */
class NetworkClient(baseUrl: String = DEFAULT_BASE_URL) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val productApi: ProductApi = retrofit.create(ProductApi::class.java)

    companion object {
        const val DEFAULT_BASE_URL = "https://dummyjson.com/"
        private const val TIMEOUT_SECONDS = 15L

        /** DummyJSON returns many fields we don't model, so unknown keys must be ignored. */
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
