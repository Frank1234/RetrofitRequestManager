package com.ironflowers.rm.lib.data.connect.cache

import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class RetrofitCacheControllerTest_retrieveRemote {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var retrofitCacheController: RetrofitCacheController

    @Before
    fun setup() {
        retrofitCallManager = RetrofitCallManager()
        retrofitCacheController = RetrofitCacheController(retrofitCallManager)
    }

    @Test
    fun successResponse() {
        val observable = retrofitCacheController.retrieveRemote { cacheControlHeaderValue ->
            Single.just("Server call with Cache-Control=$cacheControlHeaderValue")
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValue { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValue { (it as RetrofitResponse.Remote.Success<String>).data == "Server call with Cache-Control=$CACHE_HEADER_FORCE_REMOTE" }
    }

    @Test
    fun failureResponse() {
        val observable = retrofitCacheController.retrieveRemote { cacheControlHeaderValue ->
            Single.error<String>(RuntimeException("Cache-Control=$cacheControlHeaderValue"))
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValue { it is RetrofitResponse.Remote.Failure<String> }
        obs.assertValue { (it as RetrofitResponse.Remote.Failure<String>).throwable::class.java == RuntimeException::class.java }
        obs.assertValueAt(0) { (it as RetrofitResponse.Remote.Failure<String>).throwable.message == "Cache-Control=$CACHE_HEADER_FORCE_REMOTE" }
    }
}