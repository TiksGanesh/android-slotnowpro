package app.slotnow.slotnowpro.domain.model

data class WorkflowResult(
    val bookingId: String,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val paidAmountPaise: Int,
    val paymentMethod: String?
)

enum class PaymentStatus {
    PENDING,
    PARTIAL,
    PAID,
    UNKNOWN
}

