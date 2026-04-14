package app.slotnow.slotnowpro.data.remote.api

import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.auth.LogoutData
import app.slotnow.slotnowpro.data.remote.dto.auth.MeData
import app.slotnow.slotnowpro.data.remote.dto.auth.RefreshTokenData
import app.slotnow.slotnowpro.data.remote.dto.auth.RequestOtpData
import app.slotnow.slotnowpro.data.remote.dto.auth.RequestOtpRequest
import app.slotnow.slotnowpro.data.remote.dto.auth.UpdateProfileData
import app.slotnow.slotnowpro.data.remote.dto.auth.UpdateProfileRequest
import app.slotnow.slotnowpro.data.remote.dto.auth.VerifyOtpData
import app.slotnow.slotnowpro.data.remote.dto.auth.VerifyOtpRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Authentication API endpoints.
 * Endpoints request-otp and verify-otp are public - no auth required.
 * Other endpoints require Bearer token authentication.
 */
interface BarberAuthApi {
    @POST("auth/request-otp")
    suspend fun requestOtp(
        @Body request: RequestOtpRequest
    ): ApiResponse<RequestOtpData>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): ApiResponse<VerifyOtpData>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") authHeader: String
    ): ApiResponse<RefreshTokenData>

    /**
     * Synchronous refresh for OkHttp Authenticator (no coroutine blocking bridge needed).
     * Passes token via Authorization header; no request body per backend contract.
     */
    @POST("auth/refresh")
    fun refreshTokenBlocking(
        @Header("Authorization") authHeader: String
    ): Call<ApiResponse<RefreshTokenData>>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<LogoutData>

    @GET("auth/me")
    suspend fun getMe(): ApiResponse<MeData>

    @POST("auth/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ApiResponse<UpdateProfileData>
}


