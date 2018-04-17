package com.ironflowers.rm.lib.data.connect.model

sealed class RetrofitResponse<T> {

    sealed class Remote<T> : RetrofitResponse<T>() {

        data class Failure<T>(val throwable: Throwable, val httpErrorCode: Int? = null) : Remote<T>()

        data class Success<T>(val data: T) : Remote<T>()
    }

    sealed class Local<T> : RetrofitResponse<T>() {

        data class Failure<T>(val throwable: Throwable) : Local<T>()

        data class Success<T>(val data: T) : Local<T>()
    }
}