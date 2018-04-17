package com.ironflowers.rm.lib.data.connect.call

import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection


@Suppress("ClassName")
class retrofitCallManager_call {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var testHttpException: HttpException

    @Before
    fun setup() {
        retrofitCallManager = RetrofitCallManager()
        testHttpException = HttpException(
                Response.error<String>(HttpURLConnection.HTTP_NOT_FOUND,
                        ResponseBody.create(
                                MediaType.parse("TestMediaType"),
                                "TestMessage")))
    }

    @Test
    fun oneRequest_NoErrors() {
        val observable = retrofitCallManager.call("myId", { Single.just("downloaded") })
        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertNoErrors()
    }

    @Test
    fun oneRequest_RemoteSuccessResponse() {
        val observable = retrofitCallManager.call("myId", { Single.just("downloaded") })
        val obs = observable.test()
        obs.awaitCount(1)
        obs.assertValues(RetrofitResponse.Remote.Success("downloaded"))
    }

    @Test
    fun oneRequest_RemoteSuccessClearsDownloadQueue() {
        val observable = retrofitCallManager.call("myId", { Single.just("downloaded") })
        val obs = observable.test()
        obs.awaitCount(1)
        assertEquals(retrofitCallManager.currentDownloads.size, 0)
    }

    @Test
    fun oneRequestDelayed_NoErrors() {
        val testScheduler = TestScheduler()
        val observable = retrofitCallManager.call("myId", {
            Single.just("downloaded")
                    .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
        })
        val obs = observable.test()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        obs.assertNoErrors()
    }

    @Test
    fun oneRequestDelayed_RemoteSuccessResponse() {
        val testScheduler = TestScheduler()
        val observable = retrofitCallManager.call("myId", {
            Single.just("downloaded")
                    .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
        })
        val obs = observable.test()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        obs.assertValues(RetrofitResponse.Remote.Success("downloaded"))
    }

    @Test
    fun oneRequestDelayed_RemoteSuccessClearsDownloadQueue() {
        val testScheduler = TestScheduler()
        retrofitCallManager.call("myId", {
            Single.just("downloaded")
                    .delaySubscription(100, TimeUnit.MILLISECONDS, testScheduler)
        }).test()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        assertEquals(retrofitCallManager.currentDownloads.size, 0)
    }

    @Test
    fun oneRequestError_ExceptionIsCaught() {
        val observable = retrofitCallManager.call("myId", {
            Single.error<String> { testHttpException }
        })
        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertNoErrors()
    }

    @Test
    fun oneRequestError_ClearsDownloadQueue() {
        val observable = retrofitCallManager.call("myId", {
            Single.error<String> { testHttpException }
        })
        val obs = observable.test()
        obs.awaitTerminalEvent()
        assertEquals(retrofitCallManager.currentDownloads.size, 0)
    }

    @Test
    fun oneRequestError_RemoteErrorResponse() {
        val observable = retrofitCallManager.call("myId", {
            Single.error<String> { testHttpException }
        })
        val obs = observable.test()
        obs.awaitTerminalEvent()
        obs.assertValues(RetrofitResponse.Remote.Failure(testHttpException, 404))
    }

    @Test
    fun twoSimilarRequests_OneDownload() {
        retrofitCallManager.call("myId", { Single.just("downloaded1") })
        retrofitCallManager.call("myId", { Single.just("downloaded2") })
        assertEquals(retrofitCallManager.currentDownloads.size, 1)
    }

    @Test
    fun twoSimilarRequests_SameResponse() {
        retrofitCallManager.call("myId", { Single.just("downloaded1") })
        val obs = retrofitCallManager.call("myId", { Single.just("downloaded2") }).test()
        obs.awaitTerminalEvent()
        obs.assertValues(RetrofitResponse.Remote.Success("downloaded1"))
    }

    @Test
    fun twoSimilarRequestsSequence_DifferentResponses() {
        val obs1 = retrofitCallManager.call("myId", { Single.just("downloaded1") }).test()
        obs1.awaitTerminalEvent()
        obs1.assertValues(RetrofitResponse.Remote.Success("downloaded1"))
        val obs2 = retrofitCallManager.call("myId", { Single.just("downloaded2") }).test()
        obs2.awaitTerminalEvent()
        obs2.assertValues(RetrofitResponse.Remote.Success("downloaded2"))
    }

    @Test
    fun twoDifferentRequests_DifferentResponses() {
        val obs1 = retrofitCallManager.call("myId1", { Single.just("downloaded1") }).test()
        val obs2 = retrofitCallManager.call("myId2", { Single.just("downloaded2") }).test()
        obs1.awaitTerminalEvent()
        obs2.awaitTerminalEvent()
        obs1.assertValues(RetrofitResponse.Remote.Success("downloaded1"))
        obs2.assertValues(RetrofitResponse.Remote.Success("downloaded2"))
    }

    @Test
    fun twoRequestsOneError_DownloadQueueSizeOne() {
        val observable1 = retrofitCallManager.call("myId", {
            Single.error<String> { testHttpException }
        })
        retrofitCallManager.call("myId2", { Single.just("downloaded2") })
        assertEquals(retrofitCallManager.currentDownloads.size, 2)
        observable1.test().awaitTerminalEvent()
        assertEquals(retrofitCallManager.currentDownloads.size, 1)
    }
}