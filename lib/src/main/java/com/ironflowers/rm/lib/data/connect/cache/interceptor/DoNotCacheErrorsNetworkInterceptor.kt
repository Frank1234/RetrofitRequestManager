package com.ironflowers.rm.lib.data.connect.cache.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that makes sure we do not save errors in the cache.
 */
class DoNotCacheErrorsNetworkInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response? {

        var response: Response? = null
        val request = chain.request()

        // First try the regular (network) request
        try {
            response = chain.proceed(request)

            // return the original response only if it succeeds
            return if (response.isSuccessful) {
                response
            } else {
                throw IOException("Response unsuccessful, wrong response code: " + response.code())
            }
        } catch (e: Exception) {
            Log.d("RetrofitCacheIntercept", String.format("Original request error: %s [%s]",
                    request.url(), e.message))
        }

        if (response?.isSuccessful == false) {
            // we do not want to store errors:
            return response.newBuilder().addHeader("Cache-Control", "no-store").build()
        } else {
            return response
        }
    }
}
