package app.slotnow.slotnowpro.util

import app.slotnow.slotnowpro.data.remote.interceptor.NoInternetException
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRetryTest {

    @Test
    fun `executeWithIoRetry retries IOException and succeeds within max attempts`() = runTest {
        var attempts = 0

        val result = executeWithIoRetry(maxAttempts = 3) {
            attempts += 1
            if (attempts < 3) {
                throw IOException("offline")
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `executeWithIoRetry does not retry non-IOException`() = runTest {
        var attempts = 0

        val throwable = try {
            executeWithIoRetry(maxAttempts = 3) {
                attempts += 1
                throw IllegalStateException("boom")
            }
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(throwable is IllegalStateException)
        assertEquals(1, attempts)
    }

    @Test
    fun `executeWithIoRetry enforces maximum of three attempts regardless of requested value`() = runTest {
        var attempts = 0

        val throwable = try {
            executeWithIoRetry(maxAttempts = 10) {
                attempts += 1
                throw IOException("still offline")
            }
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(throwable is IOException)
        assertEquals(3, attempts)
    }

    @Test
    fun `executeWithIoRetry does not retry NoInternetException`() = runTest {
        var attempts = 0

        val throwable = try {
            executeWithIoRetry(maxAttempts = 3) {
                attempts += 1
                throw NoInternetException()
            }
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(throwable is NoInternetException)
        assertEquals(1, attempts)
    }
}

