package com.ironflowers.rm.lib.data.connect.cache

import com.ironflowers.rm.lib.data.connect.cache.interceptor.DoNotCacheErrorsNetworkInterceptor
import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit


class RetrofitCacheControllerTest_retrieveLocal_withMockedServer {

    lateinit var retrofitCallManager: RetrofitCallManager
    lateinit var cacheController: CacheController

    val mockServer = MockWebServer()
    lateinit var exampleCachingApi: ExampleCachingApi
    lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {

        mockServer.start()
        okHttpClient = OkHttpClient.Builder()
                .cache(Cache(File("test"), 1024 * 1024 * 4L))
                .addNetworkInterceptor(DoNotCacheErrorsNetworkInterceptor())
                .build()
        val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(mockServer.url("/"))
                .client(okHttpClient)
                .build()

        retrofitCallManager = RetrofitCallManager()
        cacheController = RetrofitCacheController(retrofitCallManager)
        exampleCachingApi = retrofit.create(ExampleCachingApi::class.java)
    }

    @After
    @Throws
    fun tearDown() {
        okHttpClient.cache().delete()
        mockServer.shutdown()
    }

    @Test
    fun successResponse_FirstRemoteThenLocal() {

        val responseBody = "123"

        // create the cache (with a remote call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(responseBody))
        cacheController
                .retrieveRemote { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        // fetch the cache (with a local call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(404))
        // should never be sent back to us because we will get the cached response
        val testObserver = cacheController
                .retrieveLocal { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        testObserver.awaitTerminalEvent(2, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)

        testObserver.assertValue { it -> it is RetrofitResponse.Local.Success<ResponseBody> }
        testObserver.assertValue { it -> (it as RetrofitResponse.Local.Success<ResponseBody>).data.string() == responseBody }
    }

    @Test
    fun RemoteFailureLocalFailure_LocalGives504() {

        // errors are not added to cache, but try one to prove this:
        mockServer.enqueue(MockResponse()
                .setResponseCode(404))
        cacheController
                .retrieveRemote { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        // fetch the cache (with a local call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(401))
        val testObserver = cacheController
                .retrieveLocal { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        testObserver.awaitTerminalEvent(2, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)

        testObserver.assertValue { it -> it is RetrofitResponse.Local.Failure<ResponseBody> }
        testObserver.assertValue { it -> (it as RetrofitResponse.Local.Failure<ResponseBody>).throwable is HttpException }
        testObserver.assertValue { it ->
            ((it as RetrofitResponse.Local.Failure<ResponseBody>).throwable as HttpException)
                    .code() == 504 // 504 = no cache found
        }
    }

    @Test
    fun successResponse_NoCache() {

        // fetch the cache (with a local call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody("willnotarrive")) // should never be sent back to us because there is no cached response

        val testObserver = cacheController
                .retrieveLocal { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        testObserver.awaitTerminalEvent(2, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)

        testObserver.assertValue { it -> it is RetrofitResponse.Local.Failure<ResponseBody> }
        testObserver.assertValue { it -> (it as RetrofitResponse.Local.Failure<ResponseBody>).throwable is HttpException }
        testObserver.assertValue { it ->
            ((it as RetrofitResponse.Local.Failure<ResponseBody>).throwable as HttpException)
                    .code() == 504 // 504 = only if cached
        }
    }
}