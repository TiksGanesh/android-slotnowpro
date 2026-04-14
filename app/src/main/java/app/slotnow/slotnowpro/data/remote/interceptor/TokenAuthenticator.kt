package app.slotnow.slotnowpro.data.remote.interceptor

import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.data.remote.api.BarberAuthApi
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import kotlin.concurrent.withLock
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

/**
 * OkHttp Authenticator that handles token refresh on 401 responses.
 * Uses ReentrantLock to prevent concurrent refresh attempts (single-flight pattern).
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: BarberAuthApi
) : Authenticator {

    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once to avoid infinite loops
        if (response.request.header("X-Retry-After-Refresh") != null) {
            return null
        }

        return lock.withLock {
            val currentToken = tokenManager.getToken() ?: return@withLock null

            // Check if token was already refreshed by concurrent request
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            if (currentToken != requestToken) {
                // Already refreshed — retry with current token
                return@withLock response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Retry-After-Refresh", "true")
                    .build()
            }

            // Attempt to refresh token synchronously in OkHttp worker thread
            val refreshResponse = try {
                authApi
                    .refreshTokenBlocking("Bearer $currentToken")
                    .execute()
            } catch (e: Exception) {
                // Network error or timeout — don't clear token, let original request fail
                // so it can be retried later when network recovers
                return@withLock null
            }

            when {
                // Refresh succeeded — save new token and retry request
                refreshResponse.isSuccessful -> {
                    val refreshBody = refreshResponse.body()
                    if (refreshBody?.success == true && refreshBody.data != null) {
                        val refreshData = refreshBody.data
                        val expiresAt = Instant.parse(refreshData.expiresAt)
                        tokenManager.saveToken(refreshData.token, expiresAt)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${refreshData.token}")
                            .header("X-Retry-After-Refresh", "true")
                            .build()
                    } else {
                        // Success HTTP but invalid body — transient issue, don't clear token
                        null
                    }
                }
                // 401/403 indicates token is invalid/revoked — clear and signal logout
                refreshResponse.code() == 401 || refreshResponse.code() == 403 -> {
                    tokenManager.clearToken()
                    null
                }
                // 5xx or other server error — transient, don't clear token
                refreshResponse.code() >= 500 -> {
                    null
                }
                // 4xx client error other than 401/403 — could be malformed request, don't clear token
                else -> {
                    null
                }
            }
        }
    }
}




