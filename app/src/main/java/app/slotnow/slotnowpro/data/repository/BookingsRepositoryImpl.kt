package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.BarberDashboardApi
import app.slotnow.slotnowpro.data.remote.dto.ApiErrorBody
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingDto
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingsListData
import app.slotnow.slotnowpro.domain.model.Booking
import app.slotnow.slotnowpro.domain.model.BookingStatus
import app.slotnow.slotnowpro.domain.model.DayBookings
import app.slotnow.slotnowpro.domain.repository.BookingsRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import app.slotnow.slotnowpro.util.executeWithIoRetry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

private const val BOOKINGS_REPOSITORY_LOG_TAG = "BookingsRepositoryImpl"

@Singleton
class BookingsRepositoryImpl @Inject constructor(
    private val dashboardApi: BarberDashboardApi,
    private val gson: Gson,
    private val appLogger: AppLogger
) : BookingsRepository {

    private companion object {
        private const val GET_BOOKINGS_IO_ATTEMPTS = 3
    }

    override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
        return try {
            val response = executeWithIoRetry(maxAttempts = GET_BOOKINGS_IO_ATTEMPTS) {
                dashboardApi.getBookings(date = date.toString())
            }

            if (response.success && response.data != null) {
                ApiResult.Success(response.toDomainModel())
            } else {
                ApiResult.ApiError(
                    code = response.error?.code ?: "UNKNOWN_ERROR",
                    message = response.error?.message ?: "Unable to load bookings.",
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

private fun ApiResponse<BookingsListData>.toDomainModel(): DayBookings {
    val dataValue = requireNotNull(data)
    return DayBookings(
        selectedDate = LocalDate.parse(dataValue.selectedDate),
        timezone = meta?.timezone,
        bookings = dataValue.appointments
            .map { bookingDto -> bookingDto.toDomainModel() }
            .sortedBy { booking -> booking.startTimeUtc }
    )
}

private fun BookingDto.toDomainModel(): Booking {
    val paidAmountPaise = payments
        .asSequence()
        .filter { payment -> payment.status.equals("paid", ignoreCase = true) }
        .sumOf { payment -> payment.amount }
    val pendingAmountPaise = (totalAmount - paidAmountPaise).coerceAtLeast(0)

    return Booking(
        id = id,
        customerName = customerName,
        serviceName = services?.name,
        startTimeUtc = Instant.parse(startTime),
        endTimeUtc = Instant.parse(endTime),
        totalAmountPaise = totalAmount,
        paidAmountPaise = paidAmountPaise,
        pendingAmountPaise = pendingAmountPaise,
        status = status.toBookingStatus()
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
            tag = BOOKINGS_REPOSITORY_LOG_TAG,
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

private fun parseApiError(rawErrorBody: String?, gson: Gson, appLogger: AppLogger): ApiErrorBody? {
    if (rawErrorBody.isNullOrBlank()) {
        return null
    }
    val type = object : TypeToken<ApiResponse<Any>>() {}.type
    return try {
        gson.fromJson<ApiResponse<Any>>(rawErrorBody, type).error
    } catch (parseException: Exception) {
        appLogger.logErrorDebug(
            tag = BOOKINGS_REPOSITORY_LOG_TAG,
            message = "Failed to parse API error response. Raw body (first 200 chars): ${rawErrorBody.take(200)}",
            throwable = parseException
        )
        null
    }
}


