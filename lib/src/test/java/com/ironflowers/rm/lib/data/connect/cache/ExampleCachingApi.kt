package com.ironflowers.rm.lib.data.connect.cache

import io.reactivex.Single
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header

interface ExampleCachingApi {
    @GET("etag/etag")
    fun doSomeExampleGet(
            @Header("Cache-Control") cacheControlHeader: String): Single<ResponseBody> // you can use Gson, Moshi or whatever parser to let Retrofit return a parsed object instead of this Any
}