package app.slotnow.slotnowpro.data.remote.interceptor

import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.data.remote.api.BarberAuthApi
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.auth.RefreshTokenData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Response as RetrofitResponse
import java.time.Instant

class TokenAuthenticatorTest {

    private val tokenManager = mockk<TokenManager>()
    private val authApi = mockk<BarberAuthApi>()
    private val authenticator = TokenAuthenticator(tokenManager, authApi)

    @Test
    fun `returns null when request already retried`() {
        val response = unauthorizedResponse(
            request = requestWithHeaders(
                auth = "Bearer old-token",
                retried = true
            )
        )

        val result = authenticator.authenticate(null, response)

        assertNull(result)
    }

    @Test
    fun `returns null and does not refresh when current token missing`() {
        every { tokenManager.getToken() } returns null
        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer old-token"))

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 0) { authApi.refreshTokenBlocking(any()) }
    }

    @Test
    fun `uses latest token when request token is stale`() {
        every { tokenManager.getToken() } returns "new-token"

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer old-token"))
        val result = authenticator.authenticate(null, response)

        assertNotNull(result)
        assertEquals("Bearer new-token", result?.header("Authorization"))
        assertEquals("true", result?.header("X-Retry-After-Refresh"))
        verify(exactly = 0) { authApi.refreshTokenBlocking(any()) }
    }

    @Test
    fun `refreshes token and retries request when refresh succeeds`() {
        every { tokenManager.getToken() } returns "current-token"
        every { tokenManager.saveToken(any(), any()) } just runs

        val expiresAt = Instant.now().plusSeconds(3600).toString()
        val call = mockk<Call<ApiResponse<RefreshTokenData>>>()
        every { authApi.refreshTokenBlocking("Bearer current-token") } returns call
        every { call.execute() } returns RetrofitResponse.success(
            ApiResponse(
                success = true,
                data = RefreshTokenData(token = "refreshed-token", expiresAt = expiresAt)
            )
        )

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer current-token"))
        val result = authenticator.authenticate(null, response)

        assertNotNull(result)
        assertEquals("Bearer refreshed-token", result?.header("Authorization"))
        assertEquals("true", result?.header("X-Retry-After-Refresh"))
        verify { tokenManager.saveToken("refreshed-token", Instant.parse(expiresAt)) }
    }

    @Test
    fun `clears token and returns null when refresh returns 401`() {
        every { tokenManager.getToken() } returns "current-token"
        every { tokenManager.clearToken() } just runs
        val call = mockk<Call<ApiResponse<RefreshTokenData>>>()
        every { authApi.refreshTokenBlocking("Bearer current-token") } returns call
        every { call.execute() } returns RetrofitResponse.error(401, "{}".toResponseBody())

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer current-token"))
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 1) { tokenManager.clearToken() }
    }

    @Test
    fun `clears token and returns null when refresh returns 403`() {
        every { tokenManager.getToken() } returns "current-token"
        every { tokenManager.clearToken() } just runs
        val call = mockk<Call<ApiResponse<RefreshTokenData>>>()
        every { authApi.refreshTokenBlocking("Bearer current-token") } returns call
        every { call.execute() } returns RetrofitResponse.error(403, "{}".toResponseBody())

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer current-token"))
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 1) { tokenManager.clearToken() }
    }

    @Test
    fun `does not clear token and returns null on 5xx server error`() {
        every { tokenManager.getToken() } returns "current-token"
        every { tokenManager.clearToken() } just runs
        val call = mockk<Call<ApiResponse<RefreshTokenData>>>()
        every { authApi.refreshTokenBlocking("Bearer current-token") } returns call
        every { call.execute() } returns RetrofitResponse.error(503, "{}".toResponseBody())

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer current-token"))
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 0) { tokenManager.clearToken() }
    }

    @Test
    fun `does not clear token and returns null on network exception`() {
        every { tokenManager.getToken() } returns "current-token"
        every { tokenManager.clearToken() } just runs
        val call = mockk<Call<ApiResponse<RefreshTokenData>>>()
        every { authApi.refreshTokenBlocking("Bearer current-token") } returns call
        every { call.execute() } throws java.io.IOException("Network error")

        val response = unauthorizedResponse(requestWithHeaders(auth = "Bearer current-token"))
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 0) { tokenManager.clearToken() }
    }

    private fun requestWithHeaders(auth: String, retried: Boolean = false): Request {
        val builder = Request.Builder()
            .url("http://localhost/protected")
            .header("Authorization", auth)
        if (retried) {
            builder.header("X-Retry-After-Refresh", "true")
        }
        return builder.build()
    }

    private fun unauthorizedResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .body("{}".toResponseBody())
        .build()
}

