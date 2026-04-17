package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.BarberAuthApi
import app.slotnow.slotnowpro.data.remote.dto.ApiErrorBody
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.auth.RequestOtpData
import app.slotnow.slotnowpro.data.remote.dto.auth.RequestOtpRequest
import app.slotnow.slotnowpro.data.remote.dto.auth.VerifyOtpData
import app.slotnow.slotnowpro.data.remote.dto.auth.VerifyOtpRequest
import app.slotnow.slotnowpro.domain.model.AuthSession
import app.slotnow.slotnowpro.domain.model.OtpRequestInfo
import app.slotnow.slotnowpro.domain.model.ShopInfo
import app.slotnow.slotnowpro.domain.repository.AuthRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import app.slotnow.slotnowpro.util.executeWithIoRetry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Auth repository implementation with logging for API error parsing failures.
 * This helps debug unexpected API response formats.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: BarberAuthApi,
    private val gson: Gson,
    private val appLogger: AppLogger
) : AuthRepository {

    private companion object {
        private const val LOG_TAG = "AuthRepositoryImpl"
        // Configurable per API call; utility caps retries to a maximum of 3 attempts.
        private const val REQUEST_OTP_IO_ATTEMPTS = 3
        private const val VERIFY_OTP_IO_ATTEMPTS = 3
    }

    override suspend fun requestOtp(phone: String, shopSlug: String): ApiResult<OtpRequestInfo> {
        return try {
            val response = executeWithIoRetry(maxAttempts = REQUEST_OTP_IO_ATTEMPTS) {
                authApi.requestOtp(RequestOtpRequest(phone = phone, shopSlug = shopSlug))
            }
            if (response.success && response.data != null) {
                ApiResult.Success(response.data.toDomain())
            } else {
                ApiResult.ApiError(
                    code = response.error?.code ?: "UNKNOWN_ERROR",
                    message = response.error?.message ?: "Unable to request OTP.",
                    httpStatus = 200
                )
            }
        } catch (ioException: IOException) {
            ApiResult.NetworkError(ioException)
        } catch (httpException: HttpException) {
            httpException.toApiError(gson, appLogger)
        } catch (throwable: Throwable) {
            ApiResult.ApiError(
                code = "UNEXPECTED_ERROR",
                message = throwable.message ?: "Unexpected error.",
                httpStatus = 500
            )
        }
    }

    override suspend fun verifyOtp(phone: String, code: String, shopSlug: String): ApiResult<AuthSession> {
        return try {
            val response = executeWithIoRetry(maxAttempts = VERIFY_OTP_IO_ATTEMPTS) {
                authApi.verifyOtp(
                    VerifyOtpRequest(
                        phone = phone,
                        code = code,
                        shopSlug = shopSlug
                    )
                )
            }

            if (response.success && response.data != null) {
                val data = response.data
                ApiResult.Success(
                    AuthSession(
                        token = data.token,
                        expiresAt = Instant.parse(data.expiresAt),
                        shopInfo = ShopInfo(
                            shopSlug = data.barber.shopSlug,
                            shopName = data.barber.shopName,
                            shopTimezone = data.barber.shopTimezone,
                            logoUrl = data.barber.logoUrl
                        )
                    )
                )
            } else {
                ApiResult.ApiError(
                    code = response.error?.code ?: "UNKNOWN_ERROR",
                    message = response.error?.message ?: "Unable to verify OTP.",
                    httpStatus = 200
                )
            }
        } catch (ioException: IOException) {
            ApiResult.NetworkError(ioException)
        } catch (httpException: HttpException) {
            httpException.toApiError(gson, appLogger)
        } catch (throwable: Throwable) {
            ApiResult.ApiError(
                code = "UNEXPECTED_ERROR",
                message = throwable.message ?: "Unexpected error.",
                httpStatus = 500
            )
        }
    }
}

private fun HttpException.toApiError(gson: Gson, appLogger: AppLogger): ApiResult.ApiError {
    return try {
        val rawErrorBody = response()?.errorBody()?.string()
        val parsedError = parseApiError(rawErrorBody = rawErrorBody, gson = gson, appLogger = appLogger)
        ApiResult.ApiError(
            code = parsedError?.code ?: "HTTP_${code()}",
            message = parsedError?.message ?: "Request failed.",
            httpStatus = code()
        )
    } catch (exception: Exception) {
        // Error body reading/parsing failed. Log and return generic error to prevent
        // error-handling code from crashing the request path.
        appLogger.logErrorDebug(
            tag = "AuthRepositoryImpl",
            message = "Failed to read/parse error response body for HTTP ${code()}",
            throwable = exception
        )
        ApiResult.ApiError(
            code = "HTTP_${code()}",
            message = "Request failed.",
            httpStatus = code()
        )
    }
}

/**
 * Parse API error response body with logging for unexpected formats.
 * Logs parse failures to aid debugging when the API returns malformed error responses.
 */
private fun parseApiError(rawErrorBody: String?, gson: Gson, appLogger: AppLogger): ApiErrorBody? {
    if (rawErrorBody.isNullOrBlank()) {
        return null
    }
    val type = object : TypeToken<ApiResponse<Any>>() {}.type
    return try {
        gson.fromJson<ApiResponse<Any>>(rawErrorBody, type).error
    } catch (parseException: Exception) {
        // Log parse failure with context to aid debugging unexpected API response formats
        appLogger.logErrorDebug(
            tag = "AuthRepositoryImpl",
            message = "Failed to parse API error response. Raw body (first 200 chars): ${rawErrorBody.take(200)}",
            throwable = parseException
        )
        null
    }
}

private fun RequestOtpData.toDomain(): OtpRequestInfo {
    return OtpRequestInfo(
        maskedPhone = maskedPhone,
        otpExpiresInSeconds = otpExpiresInSeconds
    )
}

