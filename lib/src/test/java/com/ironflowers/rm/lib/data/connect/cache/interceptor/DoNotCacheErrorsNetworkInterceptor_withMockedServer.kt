package com.ironflowers.rm.lib.data.connect.cache.interceptor

import com.ironflowers.rm.lib.data.connect.cache.CacheController
import com.ironflowers.rm.lib.data.connect.cache.ExampleCachingApi
import com.ironflowers.rm.lib.data.connect.cache.RetrofitCacheController
import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import junit.framework.Assert.assertEquals
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit


class DoNotCacheErrorsNetworkInterceptor_withMockedServer {

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
    fun doNotCacheFailures() {

        // create the cache (with a remote call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(404))
        val testObserver = cacheController
                .retrieveRemote { cachingHeader -> exampleCachingApi.doSomeExampleGet(cachingHeader) }
                .test()

        testObserver.awaitTerminalEvent(2, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)

        assertEquals(0, okHttpClient.cache().urls().asSequence().count())
    }

    @Test
    fun cacheSuccess() {

        // create the cache (with a remote call)
        mockServer.enqueue(MockResponse()
                .setResponseCode(200).setBody("123"))
        val testObserver = exampleCachingApi
                .doSomeExampleGet("")
                .test()

        testObserver.awaitTerminalEvent(2, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)

        assertEquals(1, okHttpClient.cache().urls().asSequence().count())
    }
}