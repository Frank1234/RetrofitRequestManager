package com.ironflowers.rm.lib.data.connect.cache

import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetrofitCacheControllerTest_retrieveLocalAndRemote {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var retrofitCacheController: RetrofitCacheController

    @Before
    fun setup() {
        retrofitCallManager = RetrofitCallManager()
        retrofitCacheController = RetrofitCacheController(retrofitCallManager)
    }


    @Test
    fun localSuccessThenRemoteSuccess() {

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL) Single.just("localFetch")
            else Single.just("remoteFetch")
        }

        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertNoErrors()
        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Local.Success<String> }
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Success<String>).data == "localFetch" }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValueAt(1) { (it as RetrofitResponse.Remote.Success<String>).data == "remoteFetch" }
    }

    @Test
    fun remoteSuccessBeforeLocalSuccess_LocalShouldNotEmit() {

        val testScheduler = TestScheduler()

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL)
                Single.just("localFetch")
                        .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
            else Single.just("remoteFetch").delaySubscription(10, TimeUnit.MILLISECONDS, testScheduler)
        }

        val obs = observable.test()

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        testScheduler.triggerActions()

        obs.assertNoErrors()
        obs.assertValueCount(1)
        obs.assertValueAt(0) { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValueAt(0) { (it as RetrofitResponse.Remote.Success<String>).data == "remoteFetch" }
    }

    @Test
    fun remoteSuccessBeforeLocalFailure_LocalShouldNotEmit() {

        val testScheduler = TestScheduler()

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL)
                Single.error<String>(RuntimeException("localFetch"))
                        .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
            else Single.just("remoteFetch").delaySubscription(10, TimeUnit.MILLISECONDS, testScheduler)
        }

        val obs = observable.test()

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        testScheduler.triggerActions()

        obs.assertValueCount(1)
        obs.assertValueAt(0) { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValueAt(0) { (it as RetrofitResponse.Remote.Success<String>).data == "remoteFetch" }
    }

    @Test
    fun localSuccessRemoteFailure() {

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL) Single.just("localFetch")
            else Single.error<String>(RuntimeException("remoteFetch"))
        }

        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertNoErrors()
        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Local.Success<String> }
        obs.assertValueAt(0) { (it as RetrofitResponse.Local.Success<String>).data == "localFetch" }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Failure<String> }
    }

    @Test
    fun remoteFailureLocalSuccess() {

        val testScheduler = TestScheduler()

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_REMOTE)
                Single.error<String>(RuntimeException("remoteFetch"))
                        .delaySubscription(10, TimeUnit.MILLISECONDS, testScheduler)
            else
                Single.just("localFetch").delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
        }

        val obs = observable.test()

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        testScheduler.triggerActions()

        obs.awaitTerminalEvent()
        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Remote.Failure<String> }
        obs.assertValueAt(1) { it is RetrofitResponse.Local.Success<String> }
        obs.assertValueAt(1) { (it as RetrofitResponse.Local.Success<String>).data == "localFetch" }
    }

    @Test
    fun localFailureRemoteSuccess() {

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_REMOTE) Single.just("remoteFetch")
            else Single.error<String>(RuntimeException("localFetch"))
        }

        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertNoErrors()
        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Local.Failure<String> }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Success<String> }
        obs.assertValueAt(1) { (it as RetrofitResponse.Remote.Success<String>).data == "remoteFetch" }
    }

    @Test
    fun localFailureRemoteFailure() {

        val testScheduler = TestScheduler()

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL)
                Single.error<String>(RuntimeException("localFetch"))
                        .delaySubscription(10, TimeUnit.MILLISECONDS, testScheduler)
            else Single.error<String>(RuntimeException("remoteFetch"))
                    .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
        }

        val obs = observable.test()

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        testScheduler.triggerActions()

        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Local.Failure<String> }
        obs.assertValueAt(1) { it is RetrofitResponse.Remote.Failure<String> }
    }

    @Test
    fun remoteFailureLocalFailure() {

        val testScheduler = TestScheduler()

        val observable = retrofitCacheController.retrieveLocalAndRemote { cacheControlHeaderValue ->
            if (cacheControlHeaderValue == CACHE_HEADER_FORCE_LOCAL)
                Single.error<String>(RuntimeException("localFetch"))
                        .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
            else Single.error<String>(RuntimeException("remoteFetch"))
                    .delaySubscription(10, TimeUnit.MILLISECONDS, testScheduler)
        }

        val obs = observable.test()

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        testScheduler.triggerActions()

        obs.assertValueCount(2)
        obs.assertValueAt(0) { it is RetrofitResponse.Remote.Failure<String> }
        obs.assertValueAt(1) { it is RetrofitResponse.Local.Failure<String> }
    }
}