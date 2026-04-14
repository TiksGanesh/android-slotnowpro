package app.slotnow.slotnowpro.data.remote.dto.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Response data for GET /api/v1/barber/dashboard/bookings?date=YYYY-MM-DD
 */
data class BookingsListData(
    @SerializedName("selectedDate")
    val selectedDate: String,
    @SerializedName("appointments")
    val appointments: List<BookingDto>,
    @SerializedName("summary")
    val summary: BookingsSummary
)

/**
 * Individual booking record
 */
data class BookingDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("total_amount")
    val totalAmount: Int,
    @SerializedName("payment_status")
    val paymentStatus: String,
    @SerializedName("services")
    val services: ServiceDto?,
    @SerializedName("payments")
    val payments: List<PaymentDto>
)

/**
 * Service info within a booking
 */
data class ServiceDto(
    @SerializedName("name")
    val name: String
)

/**
 * Payment record
 */
data class PaymentDto(
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("status")
    val status: String
)

/**
 * Booking statistics summary
 */
data class BookingsSummary(
    @SerializedName("total")
    val total: Int,
    @SerializedName("pending")
    val pending: Int,
    @SerializedName("seated")
    val seated: Int,
    @SerializedName("completed")
    val completed: Int,
    @SerializedName("no_show")
    val noShow: Int,
    @SerializedName("canceled")
    val canceled: Int
)

/**
 * Request body for POST /api/v1/barber/dashboard/bookings/{bookingId}/workflow
 */
data class WorkflowRequest(
    @SerializedName("action")
    val action: String
)

/**
 * Response data for workflow action execution
 */
data class WorkflowData(
    @SerializedName("bookingId")
    val bookingId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("paymentStatus")
    val paymentStatus: String,
    @SerializedName("paidAmount")
    val paidAmount: Int,
    @SerializedName("paymentMethod")
    val paymentMethod: String? = null
)

