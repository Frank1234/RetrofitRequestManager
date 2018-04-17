package com.ironflowers.rm.lib.data.connect

import android.support.annotation.VisibleForTesting
import android.util.Log
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Single
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages download requests to the server. Makes sure downloads with similar tag's are not done twice
 * simultaneously but instead connects the second subscriber to the same observable.
 */
class RetrofitCallManager {

    /**
     * Downloads that are currently running. Key is the tag, value is the download task.
     */
    @VisibleForTesting
    val currentDownloads = ConcurrentHashMap<String, Single<out RetrofitResponse.Remote<*>>>()

    /**
     * Downloads the requested asset from the server. [okhttpDownloadAndParseHandler] is the retrofit (or other okhttp) method
     * that calls the actual server request and parses it.
     *
     * If a download for this id is already running, the single is attached to the current download.
     */
    fun <T> call(
            tag: String,
            okhttpDownloadAndParseHandler: () -> Single<T>
    ): Single<RetrofitResponse.Remote<T>> =
            currentDownloads.getOrPut(
                    tag, { createDownloadTask(tag, okhttpDownloadAndParseHandler) }) as Single<RetrofitResponse.Remote<T>> // TODO two equal tags with different response types = error.

    private fun <T> createDownloadTask(tag: String, okhttpDownloadAndParseHandler: () -> Single<T>)
            : Single<RetrofitResponse.Remote<T>> =
            okhttpDownloadAndParseHandler.invoke()
                    .map { RetrofitResponse.Remote.Success(it) as RetrofitResponse.Remote<T> }
                    .doOnError { Log.e(RetrofitCallManager::class.java.simpleName, "Converting exception to RetrofitResponse.Failure", it) }
                    .onErrorResumeNext { mapExceptionToRemoteError(it) }
                    .doOnSuccess { currentDownloads.remove(tag) } // we filtered out errors so this is always called
                    .cache() // cache() to make sure that this Single is only executed once.

    private fun <T> mapExceptionToRemoteError(t: Throwable) =
            Single.just(RetrofitResponse.Remote.Failure<T>(t, (t as? HttpException)?.code()))
}