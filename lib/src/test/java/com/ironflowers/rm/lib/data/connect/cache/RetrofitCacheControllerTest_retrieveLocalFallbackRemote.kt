package com.ironflowers.rm.lib.data.connect.cache

import com.ironflowers.rm.lib.data.connect.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class RetrofitCacheControllerTest_retrieveLocalFallbackRemote {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var retrofitCacheController: RetrofitCacheController

    @Before
    fun setup() {
        retrofitCallManager = RetrofitCallManager()
        retrofitCacheController = RetrofitCacheController(retrofitCallManager)
    }

    @Test
    fun localSuccess() {
        val observable = retrofitCacheController.retrieveLocalFallbackRemote { cacheControlHeaderValue ->
            Single.just("Cache-Control=$cacheControlHeaderValue")
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValueCount(1)
        obs.assertValue { it is RetrofitResponse.Local.Success<String> }
        obs.assertValue { (it as RetrofitResponse.Local.Success<String>).data == "Cache-Control=$CACHE_HEADER_FORCE_LOCAL" }
    }

    @Test
    fun localFailureRemoteSuccess() {
        val observable = retrofitCacheController.retrieveLocalFallbackRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_REMOTE) Single.just("Cache-Control=$cacheControlHeaderValue")
            else Single.error<String>(RuntimeException("Cache-Control=$cacheControlHeaderValue"))
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Local.Failure<String> }
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Failure<String>).throwable::class.java == RuntimeException::class.java }
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Failure<String>).throwable.message == "Cache-Control=$CACHE_HEADER_FORCE_LOCAL" }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValueAt(1) { (it as RetrofitResponse.Remote.Success<String>).data == "Cache-Control=$CACHE_HEADER_FORCE_REMOTE" }
    }

    @Test
    fun localFailureRemoteFailure() {
        val observable = retrofitCacheController.retrieveLocalFallbackRemote { cacheControlHeaderValue ->
            Single.error<String>(RuntimeException("Cache-Control=$cacheControlHeaderValue"))
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValueCount(2)
        obs.assertValueAt(0, { it is RetrofitResponse.Local.Failure<String> })
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Failure<String>).throwable::class.java == RuntimeException::class.java }
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Failure<String>).throwable.message == "Cache-Control=$CACHE_HEADER_FORCE_LOCAL" }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Failure<String> }
        obs.assertValueAt(1) { (it as RetrofitResponse.Remote.Failure<String>).throwable::class.java == RuntimeException::class.java }
        obs.assertValueAt(1) { (it as RetrofitResponse.Remote.Failure<String>).throwable.message == "Cache-Control=$CACHE_HEADER_FORCE_REMOTE" }
    }
}