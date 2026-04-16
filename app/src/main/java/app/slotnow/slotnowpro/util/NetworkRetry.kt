package app.slotnow.slotnowpro.util

import app.slotnow.slotnowpro.data.remote.interceptor.NoInternetException
import java.io.IOException

private const val MAX_IO_RETRY_ATTEMPTS = 3

/**
 * Executes a suspending block with automatic retry logic for transient IO failures.
 *
 * This function attempts to execute the provided [block] up to [maxAttempts] times,
 * but caps the maximum attempts at [MAX_IO_RETRY_ATTEMPTS] to prevent excessive retries.
 *
 * **Retry behavior:**
 * - [IOException] exceptions trigger automatic retries (excluding [NoInternetException])
 * - [NoInternetException] is immediately rethrown without retry
 * - All other exceptions are immediately rethrown without retry
 * - Retries occur silently; exceptions from intermediate attempts are swallowed
 *
 * @param maxAttempts the desired maximum number of attempts; will be clamped to range [1, MAX_IO_RETRY_ATTEMPTS]
 * @param block a suspending lambda that performs the operation to be executed with retry logic
 * @return the result of [block] if successful
 * @throws NoInternetException if [block] throws this exception (not retried)
 * @throws IOException if all retry attempts fail with IO errors
 * @throws Exception any other exception from [block] is immediately rethrown
 */
suspend fun <T> executeWithIoRetry(
    maxAttempts: Int,
    block: suspend () -> T
): T {
    val boundedAttempts = maxAttempts.coerceIn(1, MAX_IO_RETRY_ATTEMPTS)

    repeat(boundedAttempts - 1) {
        try {
            return block()
        } catch (noInternetException: NoInternetException) {
            throw noInternetException
        } catch (_: IOException) {
            // Retry IOException only.
        }
    }

    return block()
}


