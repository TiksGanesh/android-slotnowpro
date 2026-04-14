package app.slotnow.slotnowpro.data.remote.api

import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.onboarding.ShopValidateData
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Onboarding API - no authentication required.
 * Uses plain OkHttpClient without token interceptor.
 */
interface OnboardingApi {
    @GET("shop/{slug}/validate")
    suspend fun validateShop(
        @Path("slug") slug: String
    ): ApiResponse<ShopValidateData>
}


