package app.slotnow.slotnowpro.util

import java.io.IOException

/**
 * Sealed class wrapping API call results.
 * Enforces exhaustive when() handling in calling code.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ApiError(
        val code: String,
        val message: String,
        val httpStatus: Int
    ) : ApiResult<Nothing>()
    data class NetworkError(val cause: IOException) : ApiResult<Nothing>()
}

