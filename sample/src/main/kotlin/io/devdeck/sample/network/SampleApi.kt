@file:SuppressLint("LogNotTimber")

package io.devdeck.sample.network

import android.annotation.SuppressLint
import android.util.Log
import io.devdeck.okhttp.DevDeckOkHttpInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

object SampleApi {

    private const val BASE_URL = "https://httpbin.org"

    private val json = "application/json".toMediaType()

    private val _inFlight = MutableStateFlow(0)
    val inFlight: StateFlow<Int> = _inFlight.asStateFlow()

    private val _lastResult = MutableStateFlow<String?>(null)
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    private val simulatedLatency = Interceptor { chain ->
        if (chain.request().url.encodedPath.endsWith("/search")) Thread.sleep(SEARCH_LATENCY_MILLIS)
        chain.proceed(chain.request())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(DevDeckOkHttpInterceptor())
        .addInterceptor(simulatedLatency)
        .build()

    fun loadProfile() = send(
        "Load profile",
        Request.Builder()
            .url("$BASE_URL/anything/users/42/profile?include=orders,addresses")
            .header("X-Request-Id", "sample-123")
            .header("Authorization", "Bearer sample-token")
            .build(),
    )

    fun placeOrder() = send(
        "Place order",
        Request.Builder()
            .url("$BASE_URL/anything/orders")
            .post("""{"sku":"AJ1-CHICAGO","size":"10","price":250.0,"express":true}""".toRequestBody(json))
            .build(),
    )

    fun listEuropeanCountries() = send(
        "List countries",
        Request.Builder()
            .url("https://countries.trevorblades.com/graphql")
            .post(
                """{"operationName":"Countries","query":"query Countries { countries(filter: {continent: {eq: \"EU\"}}) { code name emoji } }","variables":{}}"""
                    .toRequestBody(json),
            )
            .build(),
    )

    fun loadMissingProduct() = send("Missing product", Request.Builder().url("$BASE_URL/catalog/products/999").build())

    fun searchProducts() = send("Slow search", Request.Builder().url("$BASE_URL/anything/search?q=jordan&page=1").build())

    fun checkPaymentsHealth() = send("Payments health", Request.Builder().url("https://payments.devdeck.invalid/v1/health").build())

    private fun send(label: String, request: Request) {
        val startedAt = System.nanoTime()
        _inFlight.update { it + 1 }
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "${request.method} ${request.url.encodedPath} failed", e)
                    finish("$label → failed: ${e.javaClass.simpleName}", startedAt)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i(TAG, "${request.method} ${request.url.encodedPath} → ${response.code}")
                    response.use { it.body?.bytes() }
                    finish("$label → ${response.code}", startedAt)
                }
            },
        )
    }

    private fun finish(result: String, startedAt: Long) {
        val millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        _lastResult.value = "$result in ${millis / 1000.0} s"
        _inFlight.update { it - 1 }
    }

    private const val SEARCH_LATENCY_MILLIS = 2_000L
    private const val TAG = "SampleApi"
}
