package com.vplayed.core.error

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Failures the UI can explain to the user. Data layers convert exceptions with [toAppError]. */
sealed interface AppError {
    /** No connection, timeout or DNS failure. Retrying may help. */
    data object Network : AppError

    /** Session missing or expired (HTTP 401). */
    data object Unauthorized : AppError

    /** The server answered with an error status (HTTP or API envelope). */
    data class Server(val code: Int, val message: String? = null) : AppError

    /** The response could not be parsed into the expected model. */
    data object Parsing : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}

private const val HTTP_UNAUTHORIZED = 401

fun Throwable.toAppError(): AppError = when (this) {
    is ApiException -> if (code == HTTP_UNAUTHORIZED) AppError.Unauthorized else AppError.Server(code, message)
    is HttpException -> if (code() == HTTP_UNAUTHORIZED) AppError.Unauthorized else AppError.Server(code(), message())
    is SerializationException -> AppError.Parsing
    is IOException -> AppError.Network
    else -> AppError.Unknown(this)
}

/** An API envelope that reported failure although the HTTP call succeeded. */
class ApiException(val code: Int, override val message: String?) : Exception(message)

/** Result of a data-layer call: either data or a typed [AppError]. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>
}

/**
 * Runs [block] and converts any failure into [DataResult.Failure]. Coroutine cancellation is
 * rethrown so structured concurrency keeps working.
 */
suspend fun <T> resultOf(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    DataResult.Failure(error.toAppError())
}
