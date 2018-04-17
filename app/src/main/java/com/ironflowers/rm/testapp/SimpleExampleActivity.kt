package com.ironflowers.rm.testapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.cache.CacheController
import com.ironflowers.rm.lib.data.connect.cache.RetrofitCacheController
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

const val OKHTTP_CACHE_SIZE_BYTES = 1024 * 1024 * 4L
const val API_URL = "https://makezine.com"

class SimpleExampleActivity : AppCompatActivity() {

    // Create an OkHttp client.
    val cachingEnabledfOkHttpClient by lazy {
        OkHttpClient.Builder()
                .cache(Cache(getCacheDir(), OKHTTP_CACHE_SIZE_BYTES)) // enable caching
                .build()
    }

    // Create a very simple REST adapter which points to an example API.
    val retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(API_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // so we can use Retrofit with Reactive patterns
                .addConverterFactory(MoshiConverterFactory.create()) // use any converter you like (Gson, Moshi etc.)
                .client(cachingEnabledfOkHttpClient)
                .build()
    }

    // Create an instance of our GitHub API interface.
    val myApi by lazy { retrofit.create(ExampleCachingInterface::class.java) }

    interface ExampleCachingInterface {
        @GET("feed")
        fun doSomeExampleGet(
                @Header("Cache-Control") cacheControlHeader: String): Single<Any> // you can use Gson, Moshi or whatever parser to let Retrofit return a parsed object instead of this Any
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        runExample()
    }

    fun runExample() {

        // Create our cache controller.
        val retrofitCacheController: CacheController = RetrofitCacheController(RetrofitCallManager())

        // Create and do the call:
        val myApiObserver = retrofitCacheController
                .retrieveLocalAndRemote(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                }

        // Subscribe and handle response:
        myApiObserver
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { response: RetrofitResponse<Any> ->
                            // [Any] because [myApi] returns [Any]
                            // This may be called multiple times (once for cache, once for server), do something with the response here:
                            findViewById<TextView>(R.id.my_tv).append("\n\nReceived response object ${response.javaClass.canonicalName}: $response")
                        }
                )
    }
}
