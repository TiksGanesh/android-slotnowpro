package app.slotnow.slotnowpro.domain.model

import java.time.Instant
import java.time.LocalDate

data class Booking(
    val id: String,
    val customerName: String,
    val serviceName: String?,
    val startTimeUtc: Instant,
    val endTimeUtc: Instant,
    val totalAmountPaise: Int,
    val paidAmountPaise: Int = 0,
    val pendingAmountPaise: Int = totalAmountPaise,
    val status: BookingStatus
)

enum class BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    SEATED,
    COMPLETED,
    CANCELED,
    NO_SHOW,
    UNKNOWN
}

data class DayBookings(
    val selectedDate: LocalDate,
    val timezone: String?,
    val bookings: List<Booking>
)

