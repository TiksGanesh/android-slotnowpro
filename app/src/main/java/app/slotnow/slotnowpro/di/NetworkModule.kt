package app.slotnow.slotnowpro.di

import android.content.Context
import android.content.pm.ApplicationInfo
import app.slotnow.slotnowpro.BuildConfig
import app.slotnow.slotnowpro.data.local.prefs.LanguageManager
import app.slotnow.slotnowpro.data.local.prefs.TokenManager
import app.slotnow.slotnowpro.data.remote.api.BarberAuthApi
import app.slotnow.slotnowpro.data.remote.api.BarberDashboardApi
import app.slotnow.slotnowpro.data.remote.api.OnboardingApi
import app.slotnow.slotnowpro.data.remote.interceptor.AuthInterceptor
import app.slotnow.slotnowpro.data.remote.interceptor.TokenAuthenticator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies: Retrofit, OkHttp, API services.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL for Barber API. Defaults to HTTPS production endpoint.
     * Can be overridden via BuildConfig.API_BASE_URL for dev/staging.
     */
    private fun provideBaseUrl(): String {
        return BuildConfig.API_BASE_URL
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager = TokenManager(context)

    @Provides
    @Singleton
    fun provideLanguageManager(
        @ApplicationContext context: Context
    ): LanguageManager = LanguageManager(context)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager,
        languageManager: LanguageManager
    ): AuthInterceptor = AuthInterceptor(tokenManager, languageManager)

    /**
     * Plain Retrofit without auth interceptor — used for:
     * - Onboarding endpoints (public)
     * - Token refresh (needs special handling in TokenAuthenticator)
     */
    @Provides
    @Singleton
    @Named("plain_retrofit")
    fun providePlainRetrofit(gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(provideBaseUrl())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    /**
     * OnboardingApi uses plain Retrofit (no token interceptor)
     */
    @Provides
    @Singleton
    fun provideOnboardingApi(
        @Named("plain_retrofit") retrofit: Retrofit
    ): OnboardingApi = retrofit.create(OnboardingApi::class.java)

    /**
     * TokenAuthenticator for handling 401 responses and token refresh
     */
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        @Named("plain_retrofit") plainRetrofit: Retrofit
    ): TokenAuthenticator = TokenAuthenticator(
        tokenManager = tokenManager,
        authApi = plainRetrofit.create(BarberAuthApi::class.java)
    )

    /**
     * Main OkHttpClient with auth interceptor and token authenticator
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // Keep detailed HTTP logs in debug only; avoid sensitive data exposure in release.
                val isDebuggable =
                    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                level = if (isDebuggable) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                redactHeader("Authorization")
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Main Retrofit instance with auth-enabled OkHttpClient
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(provideBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideBarberAuthApi(retrofit: Retrofit): BarberAuthApi =
        retrofit.create(BarberAuthApi::class.java)

    @Provides
    @Singleton
    fun provideBarberDashboardApi(retrofit: Retrofit): BarberDashboardApi =
        retrofit.create(BarberDashboardApi::class.java)
}


