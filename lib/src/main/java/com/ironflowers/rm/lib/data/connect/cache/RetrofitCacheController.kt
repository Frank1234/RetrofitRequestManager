package com.ironflowers.rm.lib.data.connect.cache

import android.net.http.HttpResponseCache
import android.support.annotation.VisibleForTesting
import com.ironflowers.rm.lib.data.connect.RetrofitCallManager
import com.ironflowers.rm.lib.data.connect.model.RetrofitResponse
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.internal.operators.single.SingleInternalHelper.toObservable
import java.util.*
import java.util.Locale.filter

@VisibleForTesting
const val CACHE_HEADER_FORCE_LOCAL = "only-if-cached, max-stale=" + Integer.MAX_VALUE
@VisibleForTesting
const val CACHE_HEADER_FORCE_REMOTE = "no-cache"

/**
 * Controls remote and local storage retrieval. Eases the use of certain caching policies.
 */
class RetrofitCacheController(private val callManager: RetrofitCallManager) : CacheController {

    override fun <T> retrieveLocal(tag: String, httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Single<RetrofitResponse.Local<T>> =
            httpHandler.invoke(CACHE_HEADER_FORCE_LOCAL)
                    .map { RetrofitResponse.Local.Success(it) as RetrofitResponse.Local<T> }
                    .onErrorResumeNext { Single.just(RetrofitResponse.Local.Failure(it)) }

    override fun <T> retrieveRemote(tag: String, httpHandler: (cacheControlHeaderValue: String) -> Single<T>) =
            callManager.call(tag) { httpHandler.invoke(CACHE_HEADER_FORCE_REMOTE) }

    override fun <T> retrieveRemoteFallbackLocal(tag: String,
                                                 httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Flowable<RetrofitResponse<T>> =
            retrieveRemote(tag, httpHandler)
                    .toFlowable()
                    .flatMap {
                        if (it is RetrofitResponse.Remote.Failure)
                            Single.just(it as RetrofitResponse<T>).concatWith(
                                    retrieveLocal(tag, httpHandler))
                        else
                            Flowable.just(it)
                    }

    override fun <T> retrieveLocalFallbackRemote(tag: String,
                                                 httpHandler: (cacheControlHeaderValue: String) -> Single<T>): Flowable<RetrofitResponse<T>> =
            retrieveLocal(tag, httpHandler)
                    .toFlowable()
                    .flatMap {
                        if (it is RetrofitResponse.Local.Failure)
                            Single.just(it as RetrofitResponse<T>).concatWith(
                                    retrieveRemote(tag, httpHandler))
                        else
                            Flowable.just(it)
                    }

    override fun <T> retrieveLocalAndRemote(
            tag: String,
            httpHandler: (cacheControlHeaderValue: String) -> Single<T>
    ): Flowable<RetrofitResponse<T>> {

        val localFlowable = retrieveLocal(tag, httpHandler).toFlowable()
        val remoteFlowable = retrieveRemote(tag, httpHandler).toFlowable()

        // The takeUntil here makes sure that if the server comes first with a success response, the cache fetch is cancelled and never emitted.
        return remoteFlowable.publish { remoteSubject ->
            Flowable.merge( // merge remote and local
                    remoteSubject,
                    takeUntilRemoteSuccess(localFlowable, remoteSubject)
            )
        }
    }

    /**
     * Emits from [flowable] until [remoteSubject] emits a success response. Then it stops taking from [flowable]
     * and emits from [remoteSubjet] till termination.
     */
    private fun <T> takeUntilRemoteSuccess(flowable: Flowable<out RetrofitResponse<T>>, remoteSubject: Flowable<out RetrofitResponse<T>>)
            : Flowable<out RetrofitResponse<T>> =
            flowable.takeUntil(
                    remoteSubject
                            .filter { it is RetrofitResponse.Remote.Success<T> } // stop local only on success responses
                            .mergeWith(Flowable.never()) // needed so that we don't stop the local on termination of the single (automatically done after one onNext)
            )
}