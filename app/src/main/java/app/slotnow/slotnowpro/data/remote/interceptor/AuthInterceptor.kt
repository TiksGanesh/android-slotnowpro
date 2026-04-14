package app.slotnow.slotnowpro.data.remote.interceptor

import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds:
 * - Authorization: Bearer token
 * - Accept-Language: language
 * - X-Platform: android
 * - X-App-Version: 1.0
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val languageManager: LanguageManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()

        // Replace (not append) Authorization to avoid duplicates from Retrofit @Header
        tokenManager.getToken()?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Replace Accept-Language to ensure only one value is sent
        val language = languageManager.getOrDefault()
        requestBuilder.header("Accept-Language", language)

        // Replace platform and version headers
        requestBuilder.header("X-Platform", "android")
        requestBuilder.header("X-App-Version", "1.0")

        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}


