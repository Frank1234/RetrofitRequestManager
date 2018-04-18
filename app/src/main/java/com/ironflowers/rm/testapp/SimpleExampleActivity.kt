package com.ironflowers.rm.testapp

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.ironflowers.rm.lib.data.connect.call.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.cache.CacheController
import com.ironflowers.rm.lib.data.connect.cache.RetrofitCacheController
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Flowable
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
const val API_URL = "https://httpbin.org/"

class SimpleExampleActivity : AppCompatActivity() {

    val myTextView by lazy { findViewById<TextView>(R.id.my_tv) }

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
                .addConverterFactory(MoshiConverterFactory.create().asLenient()) // use any converter you like (Gson, Moshi etc.)
                .client(cachingEnabledfOkHttpClient)
                .build()
    }

    // Create an instance of our GitHub API interface.
    val myApi by lazy { retrofit.create(ExampleCachingInterface::class.java) }


    // Create our cache controller.
    val retrofitCacheController: CacheController by lazy { RetrofitCacheController(RetrofitCallManager()) }

    interface ExampleCachingInterface {
        @GET("etag/etag")
        fun doSomeExampleGet(
                @Header("Cache-Control") cacheControlHeader: String): Single<Any> // you can use Gson, Moshi or whatever parser to let Retrofit return a parsed object instead of this Any
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }

    fun clearText() {
        myTextView.text = null
    }

    fun onLocalOnlyClicked(v: View) {
        clearText()
        subscribeAndPrintResponse(retrofitCacheController
                .retrieveLocal(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                })
    }

    fun onRemoteOnlyClicked(v: View) {
        clearText()
        subscribeAndPrintResponse(retrofitCacheController
                .retrieveRemote(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                })
    }

    fun onLocalFallbackRemoteClicked(v: View) {
        clearText()
        subscribeAndPrintResponse(retrofitCacheController
                .retrieveLocalFallbackRemote(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                })
    }

    fun onRemoteFallbackLocalClicked(v: View) {
        clearText()
        subscribeAndPrintResponse(retrofitCacheController
                .retrieveRemoteFallbackLocal(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                })
    }

    fun onRetrieveLocalAndRemoteClicked(v: View) {
        clearText()
        subscribeAndPrintResponse(retrofitCacheController
                .retrieveLocalAndRemote(API_URL) { cacheControlHeaderValue ->
                    myApi.doSomeExampleGet(cacheControlHeaderValue)
                })
    }

    @SuppressLint("SetTextI18n")
    fun onClearCacheClicked(v: View) {
        cachingEnabledfOkHttpClient.cache().evictAll()
        myTextView.text = "Cleared all cache."
    }

    private fun <T> subscribeAndPrintResponse(myApiObserver: Single<out RetrofitResponse<T>>) =
            subscribeAndPrintResponse(myApiObserver.toFlowable())

    private fun <T> subscribeAndPrintResponse(myApiObserver: Flowable<out RetrofitResponse<T>>) {
        myApiObserver.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { response: RetrofitResponse<T> ->
                            // [Any] because [myApi] returns [Any]
                            // This may be called multiple times (once for cache, once for server), do something with the response here:
                            myTextView.append("\n\nReceived response object ${response.javaClass.canonicalName}: $response")
                        }
                )
    }
}
