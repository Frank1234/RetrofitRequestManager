package com.ironflowers.rm.lib.data.connect.cache

import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class RetrofitCacheControllerTest_retrieveLocal {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var retrofitCacheController: RetrofitCacheController

    @Before
    fun setup() {
        retrofitCallManager = RetrofitCallManager()
        retrofitCacheController = RetrofitCacheController(retrofitCallManager)
    }

    @Test
    fun successResponse() {
        val observable = retrofitCacheController.retrieveLocal { cacheControlHeaderValue ->
            Single.just("Cache call with Cache-Control=$cacheControlHeaderValue")
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValue { it is RetrofitResponse.Local.Success<String> }
        obs.assertValue { (it as RetrofitResponse.Local.Success<String>).data == "Cache call with Cache-Control=$CACHE_HEADER_FORCE_LOCAL" }
    }

    @Test
    fun failureResponse() {
        val observable = retrofitCacheController.retrieveLocal { cacheControlHeaderValue ->
            Single.error<String>(RuntimeException("Cache-Control=$cacheControlHeaderValue"))
        }
        val obs = observable.test()
        obs.awaitTerminalEvent()

        obs.assertValue { it is RetrofitResponse.Local.Failure<String> }
        obs.assertValue { (it as RetrofitResponse.Local.Failure<String>).throwable::class.java == RuntimeException::class.java }
        obs.assertValue { (it as RetrofitResponse.Local.Failure<String>).throwable.message == "Cache-Control=$CACHE_HEADER_FORCE_LOCAL" }
    }
}