package app.slotnow.slotnowpro.data.remote.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.slotnow.slotnowpro.util.AppLogger
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ConnectivityInterceptorTest {

    private val context = mockk<Context>()
    private val appLogger = mockk< AppLogger>(relaxed = true)
    private val connectivityManager = mockk<ConnectivityManager>()

    @Test
    fun `throws NoInternetException when no active network`() {
        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null

        val interceptor = ConnectivityInterceptor(context, appLogger)
        val chain = CapturingChain(baseRequest())

        val throwable = try {
            interceptor.intercept(chain)
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(throwable is NoInternetException)
    }

    @Test
    fun `proceeds request when internet capability is available`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } returns true
        every {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } returns true

        val interceptor = ConnectivityInterceptor(context, appLogger)
        val chain = CapturingChain(baseRequest())
        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals("http://localhost/test", chain.capturedRequest.url.toString())
    }

    private fun baseRequest(): Request = Request.Builder()
        .url("http://localhost/test")
        .build()

    private class CapturingChain(
        private val request: Request
    ) : Interceptor.Chain {
        lateinit var capturedRequest: Request

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            capturedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }

        override fun connection() = null
        override fun call() = throw UnsupportedOperationException("Not needed")
        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain = this
    }
}

