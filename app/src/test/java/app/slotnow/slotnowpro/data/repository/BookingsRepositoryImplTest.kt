package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.BarberDashboardApi
import app.slotnow.slotnowpro.data.remote.dto.ApiMeta
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingDto
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingsListData
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingsSummary
import app.slotnow.slotnowpro.data.remote.dto.dashboard.PaymentDto
import app.slotnow.slotnowpro.data.remote.dto.dashboard.ServiceDto
import app.slotnow.slotnowpro.domain.model.BookingStatus
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingsRepositoryImplTest {

    private val api = mockk<BarberDashboardApi>()
    private val gson = Gson()
    private val appLogger = mockk<AppLogger>(relaxed = true)

    @Test
    fun `maps bookings response into day bookings domain model`() = runTest {
        coEvery { api.getBookings(date = "2026-04-17") } returns ApiResponse(
            success = true,
            data = BookingsListData(
                selectedDate = "2026-04-17",
                appointments = listOf(
                    BookingDto(
                        id = "b1",
                        customerName = "Arun Sharma",
                        customerPhone = "+919111111111",
                        startTime = "2026-04-17T03:30:00Z",
                        endTime = "2026-04-17T04:00:00Z",
                        status = "confirmed",
                        totalAmount = 40000,
                        paymentStatus = "pending",
                        services = ServiceDto(name = "Haircut"),
                        payments = listOf(PaymentDto(amount = 0, status = "pending"))
                    )
                ),
                summary = BookingsSummary(
                    total = 1,
                    pending = 1,
                    seated = 0,
                    completed = 0,
                    noShow = 0,
                    canceled = 0
                )
            ),
            meta = ApiMeta(timezone = "Asia/Kolkata")
        )

        val repository = BookingsRepositoryImpl(
            dashboardApi = api,
            gson = gson,
            appLogger = appLogger
        )

        val result = repository.getBookingsForDate(LocalDate.of(2026, 4, 17))

        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success
        assertEquals(LocalDate.of(2026, 4, 17), success.data.selectedDate)
        assertEquals("Asia/Kolkata", success.data.timezone)
        assertEquals(1, success.data.bookings.size)
        assertEquals(BookingStatus.CONFIRMED, success.data.bookings.first().status)
        assertEquals(0, success.data.bookings.first().paidAmountPaise)
        assertEquals(40000, success.data.bookings.first().pendingAmountPaise)
    }

    @Test
    fun `maps paid and pending amount from payments list`() = runTest {
        coEvery { api.getBookings(date = "2026-04-17") } returns ApiResponse(
            success = true,
            data = BookingsListData(
                selectedDate = "2026-04-17",
                appointments = listOf(
                    BookingDto(
                        id = "b1",
                        customerName = "Arun Sharma",
                        customerPhone = "+919111111111",
                        startTime = "2026-04-17T03:30:00Z",
                        endTime = "2026-04-17T04:00:00Z",
                        status = "completed",
                        totalAmount = 40000,
                        paymentStatus = "partial",
                        services = ServiceDto(name = "Haircut"),
                        payments = listOf(
                            PaymentDto(amount = 15000, status = "paid"),
                            PaymentDto(amount = 5000, status = "pending")
                        )
                    )
                ),
                summary = BookingsSummary(
                    total = 1,
                    pending = 0,
                    seated = 0,
                    completed = 1,
                    noShow = 0,
                    canceled = 0
                )
            )
        )

        val repository = BookingsRepositoryImpl(
            dashboardApi = api,
            gson = gson,
            appLogger = appLogger
        )

        val result = repository.getBookingsForDate(LocalDate.of(2026, 4, 17))

        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success
        assertEquals(15000, success.data.bookings.first().paidAmountPaise)
        assertEquals(25000, success.data.bookings.first().pendingAmountPaise)
    }

    @Test
    fun `maps unknown status to UNKNOWN enum`() = runTest {
        coEvery { api.getBookings(date = "2026-04-17") } returns ApiResponse(
            success = true,
            data = BookingsListData(
                selectedDate = "2026-04-17",
                appointments = listOf(
                    BookingDto(
                        id = "b1",
                        customerName = "Arun Sharma",
                        customerPhone = "+919111111111",
                        startTime = "2026-04-17T03:30:00Z",
                        endTime = "2026-04-17T04:00:00Z",
                        status = "rescheduled",
                        totalAmount = 40000,
                        paymentStatus = "pending",
                        services = null,
                        payments = emptyList()
                    )
                ),
                summary = BookingsSummary(
                    total = 1,
                    pending = 0,
                    seated = 0,
                    completed = 0,
                    noShow = 0,
                    canceled = 0
                )
            )
        )

        val repository = BookingsRepositoryImpl(
            dashboardApi = api,
            gson = gson,
            appLogger = appLogger
        )

        val result = repository.getBookingsForDate(LocalDate.of(2026, 4, 17))

        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success
        assertEquals(BookingStatus.UNKNOWN, success.data.bookings.first().status)
    }
}

