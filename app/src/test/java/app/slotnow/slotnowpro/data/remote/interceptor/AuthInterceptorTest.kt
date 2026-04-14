package app.slotnow.slotnowpro.data.remote.interceptor

import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private val tokenManager = mockk<TokenManager>()
    private val languageManager = mockk<LanguageManager>()
    private val interceptor = AuthInterceptor(tokenManager, languageManager)

    @Test
    fun `adds all expected headers when token exists`() {
        every { tokenManager.getToken() } returns "token-123"
        every { languageManager.getOrDefault() } returns "hi"

        val chain = CapturingChain(baseRequest())
        interceptor.intercept(chain)

        val sent = chain.capturedRequest
        assertEquals("Bearer token-123", sent.header("Authorization"))
        assertEquals("hi", sent.header("Accept-Language"))
        assertEquals("android", sent.header("X-Platform"))
        assertEquals("1.0", sent.header("X-App-Version"))
    }

    @Test
    fun `does not add authorization header when token missing`() {
        every { tokenManager.getToken() } returns null
        every { languageManager.getOrDefault() } returns "en"

        val chain = CapturingChain(baseRequest())
        interceptor.intercept(chain)

        assertNull(chain.capturedRequest.header("Authorization"))
        assertEquals("en", chain.capturedRequest.header("Accept-Language"))
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
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }
}

