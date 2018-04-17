package com.ironflowers.rm.lib.data.connect.cache

import android.net.http.HttpResponseCache
import android.support.annotation.VisibleForTesting
import com.ironflowers.rm.lib.data.connect.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.internal.operators.single.SingleInternalHelper.toObservable
import java.util.*
import java.util.Locale.filter

/**
 * Controls remote and local storage retrieval. Eases the use of certain caching policies.
 */
interface CacheController {

    /**
     * Fetches data from local cache.
     *
     * @param httpHandler method that does the actual api call. [cacheControlHeaderValue] is the value to use in
     * the cache control header of your retrofit or other http call (in retrofit, the value to set on: @Header("Cache-Control")).
     * @param tag an optional tag. If you start two calls with the same tag, the call will only be done once, and
     * both subscribers will get the same result. Leave null to do a call every time.
     */
    fun <T> retrieveLocal(tag: String = UUID.randomUUID().toString(),
                          httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Single<RetrofitResponse.Local<T>>

    /**
     * Fetches data from remote server.
     *
     * @param httpHandler method that does the actual api call. [cacheControlHeaderValue] is the value to use in
     * the cache control header of your retrofit or other http call (in retrofit, the value to set on: @Header("Cache-Control")).
     * @param tag an optional tag. If you start two calls with the same tag, the call will only be done once, and
     * both subscribers will get the same result. Leave null to do a call every time.
     */
    fun <T> retrieveRemote(tag: String = UUID.randomUUID().toString(),
                           httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Single<RetrofitResponse.Remote<T>>

    /**
     * Fetches data from remote server. On failure, it tries to fetch a cached response.
     *
     * @param httpHandler method that does the actual api call. [cacheControlHeaderValue] is the value to use in
     * the cache control header of your retrofit or other http call (in retrofit, the value to set on: @Header("Cache-Control")).
     * @param tag an optional tag. If you start two calls with the same tag, the call will only be done once, and
     * both subscribers will get the same result. Leave null to do a call every time.
     */
    fun <T> retrieveLocalFallbackRemote(tag: String = UUID.randomUUID().toString(),
                                        httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Flowable<RetrofitResponse<T>>

    /**
     * Fetches data from local cache. On failure / not found, it tries to fetch it from the server.
     *
     * @param httpHandler method that does the actual api call. [cacheControlHeaderValue] is the value to use in
     * the cache control header of your retrofit or other http call (in retrofit, the value to set on: @Header("Cache-Control")).
     * @param tag an optional tag. If you start two calls with the same tag, the call will only be done once, and
     * both subscribers will get the same result. Leave null to do a call every time.
     */
    fun <T> retrieveRemoteFallbackLocal(tag: String = UUID.randomUUID().toString(),
                                        httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Flowable<RetrofitResponse<T>>

    /**
     * Fetches data from cache and server at the same time. May emit multiple success and/or failure responses.
     *
     * Usually the cache response will be emitted first. In case the server passes the cache-request (in time)
     * with a successful response, than we cancel the cache-request. Server errors do not interfere with the
     * cache-fetch. All results, either local or emote success and error states, are emitted down the stream.
     *
     * @param httpHandler method that does the actual api call. [cacheControlHeaderValue] is the value to use in
     * the cache control header of your retrofit or other http call (in retrofit, the value to set on: @Header("Cache-Control")).
     * @param tag an optional tag. If you start two calls with the same tag, the call will only be done once, and
     * both subscribers will get the same result. Leave null to do a call every time.
     */
    fun <T>
            retrieveLocalAndRemote(
            tag: String = UUID.randomUUID().toString(),
            httpHandler: (cacheControlHeaderValue: String) -> Single<T>
    ): Flowable<RetrofitResponse<T>>
}