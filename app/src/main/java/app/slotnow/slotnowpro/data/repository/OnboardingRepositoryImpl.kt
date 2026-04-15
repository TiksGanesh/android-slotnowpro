package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.OnboardingApi
import app.slotnow.slotnowpro.data.remote.dto.ApiErrorBody
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.onboarding.ShopValidateData
import app.slotnow.slotnowpro.domain.model.ShopInfo
import app.slotnow.slotnowpro.domain.repository.OnboardingRepository
import app.slotnow.slotnowpro.util.ApiResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val onboardingApi: OnboardingApi,
    private val gson: Gson
) : OnboardingRepository {

    override suspend fun validateShopSlug(slug: String): ApiResult<ShopInfo> {
        return try {
            val response = onboardingApi.validateShop(slug)
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.data != null) {
                ApiResult.Success(body.data.toDomainModel())
            } else {
                val apiError = body?.error ?: parseApiError(response.errorBody()?.string())
                val fallbackCode = when (response.code()) {
                    403 -> "SHOP_INACTIVE"
                    404 -> "SHOP_NOT_FOUND"
                    else -> "HTTP_${response.code()}"
                }
                val fallbackMessage = if (response.code() >= 500) {
                    "Server error. Please retry."
                } else {
                    "Unable to validate shop."
                }
                ApiResult.ApiError(
                    code = apiError?.code ?: fallbackCode,
                    message = apiError?.message ?: fallbackMessage,
                    httpStatus = response.code()
                )
            }
        } catch (ioException: IOException) {
            ApiResult.NetworkError(ioException)
        }
    }

    private fun parseApiError(rawErrorBody: String?): ApiErrorBody? {
        if (rawErrorBody.isNullOrBlank()) {
            return null
        }
        val type = object : TypeToken<ApiResponse<Any>>() {}.type
        return try {
            gson.fromJson<ApiResponse<Any>>(rawErrorBody, type).error
        } catch (_: Exception) {
            null
        }
    }
}

private fun ShopValidateData.toDomainModel(): ShopInfo {
    return ShopInfo(
        shopSlug = shopSlug,
        shopName = shopName,
        shopTimezone = shopTimezone,
        logoUrl = logoUrl
    )
}

