package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.BarberDashboardApi
import app.slotnow.slotnowpro.data.remote.dto.ApiErrorBody
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.dashboard.WorkflowData
import app.slotnow.slotnowpro.data.remote.dto.dashboard.WorkflowRequest
import app.slotnow.slotnowpro.domain.model.BookingStatus
import app.slotnow.slotnowpro.domain.model.PaymentStatus
import app.slotnow.slotnowpro.domain.model.WorkflowAction
import app.slotnow.slotnowpro.domain.model.WorkflowResult
import app.slotnow.slotnowpro.domain.repository.WorkflowRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

private const val WORKFLOW_REPOSITORY_LOG_TAG = "WorkflowRepositoryImpl"

@Singleton
class WorkflowRepositoryImpl @Inject constructor(
    private val dashboardApi: BarberDashboardApi,
    private val gson: Gson,
    private val appLogger: AppLogger
) : WorkflowRepository {

    override suspend fun executeAction(
        bookingId: String,
        action: WorkflowAction
    ): ApiResult<WorkflowResult> {
        return try {
            val response = dashboardApi.executeWorkflow(
                bookingId = bookingId,
                request = WorkflowRequest(action = action.apiValue)
            )

            if (response.success && response.data != null) {
                ApiResult.Success(response.data.toDomainModel())
            } else {
                ApiResult.ApiError(
                    code = response.error?.code ?: "UNKNOWN_ERROR",
                    message = response.error?.message ?: "Unable to update booking.",
                    httpStatus = 200
                )
            }
        } catch (ioException: IOException) {
            ApiResult.NetworkError(ioException)
        } catch (httpException: HttpException) {
            httpException.toApiError(gson = gson, appLogger = appLogger)
        } catch (throwable: Throwable) {
            ApiResult.ApiError(
                code = "UNEXPECTED_ERROR",
                message = throwable.message ?: "Unexpected error.",
                httpStatus = 500
            )
        }
    }
}

private fun WorkflowData.toDomainModel(): WorkflowResult {
    return WorkflowResult(
        bookingId = bookingId,
        status = status.toBookingStatus(),
        paymentStatus = paymentStatus.toPaymentStatus(),
        paidAmountPaise = paidAmount,
        paymentMethod = paymentMethod
    )
}

private fun String.toBookingStatus(): BookingStatus {
    return when (this) {
        "pending_payment" -> BookingStatus.PENDING_PAYMENT
        "confirmed" -> BookingStatus.CONFIRMED
        "seated" -> BookingStatus.SEATED
        "completed" -> BookingStatus.COMPLETED
        "canceled" -> BookingStatus.CANCELED
        "no_show" -> BookingStatus.NO_SHOW
        else -> BookingStatus.UNKNOWN
    }
}

private fun String.toPaymentStatus(): PaymentStatus {
    return when (this) {
        "pending" -> PaymentStatus.PENDING
        "partial" -> PaymentStatus.PARTIAL
        "paid" -> PaymentStatus.PAID
        else -> PaymentStatus.UNKNOWN
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
        appLogger.logErrorDebug(
            tag = WORKFLOW_REPOSITORY_LOG_TAG,
            message = "Failed to read/parse workflow error response body for HTTP ${code()}",
            throwable = exception
        )
        ApiResult.ApiError(
            code = "HTTP_${code()}",
            message = "Request failed.",
            httpStatus = code()
        )
    }
}

private fun parseApiError(rawErrorBody: String?, gson: Gson, appLogger: AppLogger): ApiErrorBody? {
    if (rawErrorBody.isNullOrBlank()) {
        return null
    }
    val type = object : TypeToken<ApiResponse<Any>>() {}.type
    return try {
        gson.fromJson<ApiResponse<Any>>(rawErrorBody, type).error
    } catch (parseException: Exception) {
        appLogger.logErrorDebug(
            tag = WORKFLOW_REPOSITORY_LOG_TAG,
            message = "Failed to parse workflow API error response. Raw body (first 200 chars): ${rawErrorBody.take(200)}",
            throwable = parseException
        )
        null
    }
}

