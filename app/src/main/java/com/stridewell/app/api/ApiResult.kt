package com.stridewell.app.api

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val status: Int, val message: String) : ApiResult<Nothing>()
}

val ApiResult<*>.ok: Boolean
    get() = this is ApiResult.Success

val <T> ApiResult<T>.dataOrNull: T?
    get() = (this as? ApiResult.Success)?.data

val ApiResult<*>.errorMessage: String?
    get() = (this as? ApiResult.Error)?.message

/** True when the request failed due to no network (status == 0). */
val ApiResult<*>.isOffline: Boolean
    get() = this is ApiResult.Error && status == 0
