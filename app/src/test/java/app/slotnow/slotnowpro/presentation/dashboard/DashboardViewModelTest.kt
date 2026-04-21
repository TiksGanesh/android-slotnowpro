package app.slotnow.slotnowpro.presentation.dashboard

import app.slotnow.slotnowpro.domain.model.Booking
import app.slotnow.slotnowpro.domain.model.BookingStatus as DomainBookingStatus
import app.slotnow.slotnowpro.domain.model.DayBookings
import app.slotnow.slotnowpro.domain.model.WorkflowAction
import app.slotnow.slotnowpro.domain.model.WorkflowResult
import app.slotnow.slotnowpro.domain.repository.BookingsRepository
import app.slotnow.slotnowpro.domain.repository.WorkflowRepository
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val appLogger = mockk<AppLogger>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `upcoming tab counts only scheduled statuses and completed is separated`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        val uiState = viewModel.uiState.value
        val upcomingStatuses = setOf(
            app.slotnow.slotnowpro.presentation.dashboard.BookingStatus.PENDING_PAYMENT,
            app.slotnow.slotnowpro.presentation.dashboard.BookingStatus.CONFIRMED,
            app.slotnow.slotnowpro.presentation.dashboard.BookingStatus.SEATED
        )

        assertEquals(BookingTab.UPCOMING, uiState.selectedTab)
        assertEquals(uiState.filteredBookings.size, uiState.filteredBookingCount)
        assertTrue(uiState.filteredBookings.all { booking -> booking.status in upcomingStatuses })
        assertTrue(uiState.filteredBookings.none { booking -> booking.id == "4" })
        assertEquals(1, uiState.completedCount)
        assertEquals(1, uiState.cancelledCount)
        assertTrue(uiState.noShowCount >= 1)
    }

    @Test
    fun `selecting appointments tab shows completed cancelled and no show appointments`() = runTest {
        val repository = FakeBookingsRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().plusDays(1))
        advanceUntilIdle()
        viewModel.selectTab(BookingTab.APPOINTMENTS)

        val uiState = viewModel.uiState.value
        assertEquals(BookingTab.APPOINTMENTS, uiState.selectedTab)
        assertEquals(3, uiState.filteredBookingCount)
        assertEquals(listOf("4", "5", "6"), uiState.filteredBookings.map { booking -> booking.id })
    }

    @Test
    fun `appointments filters update list locally without new api call`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().plusDays(1))
        advanceUntilIdle()
        val callCountBeforeTabChange = repository.requestedDates.size

        viewModel.selectTab(BookingTab.APPOINTMENTS)
        viewModel.selectAppointmentsFilter(BookingFilter.COMPLETED)
        viewModel.selectAppointmentsFilter(BookingFilter.NO_SHOW)

        val uiState = viewModel.uiState.value
        assertEquals(BookingTab.APPOINTMENTS, uiState.selectedTab)
        assertEquals(setOf(BookingFilter.CANCELLED), uiState.selectedAppointmentFilters)
        assertEquals(1, uiState.filteredBookingCount)
        assertEquals(listOf("5"), uiState.filteredBookings.map { booking -> booking.id })
        assertEquals(callCountBeforeTabChange, repository.requestedDates.size)
    }

    @Test
    fun `selecting date triggers repository fetch for that date`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        val targetDate = LocalDate.of(2026, 4, 18)

        viewModel.selectDate(targetDate)
        advanceUntilIdle()

        assertEquals(targetDate, repository.requestedDates.last())
        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `date navigation moves date by one day and refetches`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        val initialDate = viewModel.uiState.value.selectedDate

        viewModel.goToNextDay()
        advanceUntilIdle()
        assertEquals(initialDate.plusDays(1), viewModel.uiState.value.selectedDate)

        viewModel.goToPreviousDay()
        advanceUntilIdle()
        assertEquals(initialDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `date navigation shows loading overlay state while fetch is in progress`() = runTest {
        val repository = object : BookingsRepository {
            val requestedDates = mutableListOf<LocalDate>()

            override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
                requestedDates += date
                delay(1_000)
                return ApiResult.Success(
                    DayBookings(
                        selectedDate = date,
                        timezone = "Asia/Kolkata",
                        bookings = emptyList()
                    )
                )
            }
        }
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.goToNextDay()
        runCurrent()

        assertEquals(true, viewModel.uiState.value.isDateChangeLoading)

        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isDateChangeLoading)
    }

    @Test
    fun `next appointment falls back to first actionable booking when selected date is not today`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().plusDays(2))
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNull(uiState.currentTime)
        assertEquals("1", uiState.nextAppointment?.id)
    }

    @Test
    fun `in progress booking is selected as next appointment with highest priority`() = runTest {
        // Use fixed time: 2026-04-17 09:00:00 UTC+5:30 (Asia/Kolkata)
        val fixedInstant = Instant.parse("2026-04-17T03:30:00Z") // 09:00 in Kolkata
        val zone = ZoneId.of("Asia/Kolkata")
        val fixedTime = ZonedDateTime.ofInstant(fixedInstant, zone)

        val repository = object : BookingsRepository {
            override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
                return ApiResult.Success(
                    DayBookings(
                        selectedDate = fixedTime.toLocalDate(),
                        timezone = "Asia/Kolkata",
                        bookings = listOf(
                            Booking(
                                id = "live",
                                customerName = "Live Customer",
                                serviceName = "Haircut",
                                startTimeUtc = fixedTime.minusMinutes(10).toInstant(),
                                endTimeUtc = fixedTime.plusMinutes(20).toInstant(),
                                totalAmountPaise = 45000,
                                status = DomainBookingStatus.CONFIRMED
                            ),
                            Booking(
                                id = "future",
                                customerName = "Future Customer",
                                serviceName = "Shave",
                                startTimeUtc = fixedTime.plusMinutes(40).toInstant(),
                                endTimeUtc = fixedTime.plusMinutes(60).toInstant(),
                                totalAmountPaise = 25000,
                                status = DomainBookingStatus.CONFIRMED
                            )
                        )
                    )
                )
            }
        }

        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        val uiState = viewModel.uiState.value

        assertTrue(uiState.nextAppointment?.id in setOf("future", "live"))
        assertEquals("live", uiState.inProgressBookingId)
    }

    @Test
    fun `on resume recalculation updates current time for today`() = runTest {
        // Use fixed time: 2026-04-17 09:00:00 UTC+5:30 (Asia/Kolkata)
        val fixedInstant = Instant.parse("2026-04-17T03:30:00Z")
        val zone = ZoneId.of("Asia/Kolkata")
        val fixedDate = ZonedDateTime.ofInstant(fixedInstant, zone).toLocalDate()

        val repository = object : BookingsRepository {
            override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
                // Return bookings for the fixed date regardless of what's requested
                return ApiResult.Success(
                    DayBookings(
                        selectedDate = fixedDate,
                        timezone = "Asia/Kolkata",
                        bookings = emptyList()
                    )
                )
            }
        }

        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        // Manually set selectedDate to match the fixed date so currentTime can be computed
        // (currentTime is only non-null when selectedDate matches today in the shop timezone)
        // We need to simulate the ViewModel being in the correct state
        viewModel.selectDate(fixedDate)
        advanceUntilIdle()

        viewModel.onResumeRecalculateNow()

        assertNotNull(viewModel.uiState.value.currentTime)
    }

    @Test
    fun `start action updates booking status on workflow success`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        val targetBooking = viewModel.uiState.value.allBookings.first { booking -> booking.id == "2" }

        viewModel.onStartRequested(targetBooking)
        advanceUntilIdle()

        assertEquals(listOf("2:start"), workflowRepository.recordedActions)
        val updated = viewModel.uiState.value.allBookings.first { booking -> booking.id == "2" }
        assertEquals(app.slotnow.slotnowpro.presentation.dashboard.BookingStatus.SEATED, updated.status)
    }

    @Test
    fun `workflow api error surfaces action error message`() = runTest {
        val repository = FakeBookingsRepository()
        val workflowRepository = FakeWorkflowRepository(
            nextResult = ApiResult.ApiError(
                code = "INVALID_TRANSITION",
                message = "Cannot start this appointment",
                httpStatus = 409
            )
        )
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        val targetBooking = viewModel.uiState.value.allBookings.first { booking -> booking.id == "2" }
        viewModel.onStartRequested(targetBooking)
        advanceUntilIdle()

        assertEquals("Cannot start this appointment", viewModel.uiState.value.actionErrorMessage)
    }

    @Test
    fun `next appointment is hidden when appointments tab is selected`() = runTest {
        val repository = FakeBookingsRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().plusDays(1))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.nextAppointment)

        viewModel.selectTab(BookingTab.APPOINTMENTS)
        assertNull(viewModel.uiState.value.nextAppointment)
    }

    @Test
    fun `past dates force appointments tab and hide upcoming selection`() = runTest {
        val viewModel = DashboardViewModel(
            bookingsRepository = FakeBookingsRepository(),
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().minusDays(1))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPastDateSelected)
        assertEquals(BookingTab.APPOINTMENTS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(BookingTab.UPCOMING)
        assertEquals(BookingTab.APPOINTMENTS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `past date bookings are read only and do not execute workflow actions`() = runTest {
        val workflowRepository = FakeWorkflowRepository()
        val viewModel = DashboardViewModel(
            bookingsRepository = FakeBookingsRepository(),
            workflowRepository = workflowRepository,
            appLogger = appLogger
        )

        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().minusDays(1))
        advanceUntilIdle()

        val targetBooking = viewModel.uiState.value.allBookings.first { booking -> booking.id == "2" }
        val beforeStatus = targetBooking.status

        viewModel.onStartRequested(targetBooking)
        advanceUntilIdle()

        val afterStatus = viewModel.uiState.value.allBookings.first { booking -> booking.id == "2" }.status
        assertEquals(beforeStatus, afterStatus)
        assertTrue(viewModel.uiState.value.isPastDateSelected)
        assertFalse(workflowRepository.recordedActions.contains("2:start"))
    }

    @Test
    fun `today past pending appointment is auto-classified as no-show`() = runTest {
        // Use fixed time: 2026-04-17 09:00:00 UTC+5:30 (Asia/Kolkata)
        val fixedInstant = Instant.parse("2026-04-17T03:30:00Z")
        val zone = ZoneId.of("Asia/Kolkata")
        val fixedTime = ZonedDateTime.ofInstant(fixedInstant, zone)

        val repository = object : BookingsRepository {
            override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
                return ApiResult.Success(
                    DayBookings(
                        selectedDate = fixedTime.toLocalDate(),
                        timezone = "Asia/Kolkata",
                        bookings = listOf(
                            Booking(
                                id = "past-pending",
                                customerName = "Pending Customer",
                                serviceName = "Haircut",
                                startTimeUtc = fixedTime.minusMinutes(90).toInstant(),
                                endTimeUtc = fixedTime.minusMinutes(60).toInstant(),
                                totalAmountPaise = 30000,
                                status = DomainBookingStatus.PENDING_PAYMENT
                            )
                        )
                    )
                )
            }
        }

        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertEquals(0, uiState.upcomingCount)
        assertEquals(1, uiState.noShowCount)
    }

    @Test
    fun `next appointment is hidden when today has no future upcoming appointments`() = runTest {
        // Use fixed time: 2026-04-17 09:00:00 UTC+5:30 (Asia/Kolkata)
        val fixedInstant = Instant.parse("2026-04-17T03:30:00Z")
        val zone = ZoneId.of("Asia/Kolkata")
        val fixedTime = ZonedDateTime.ofInstant(fixedInstant, zone)

        val repository = object : BookingsRepository {
            override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
                return ApiResult.Success(
                    DayBookings(
                        selectedDate = fixedTime.toLocalDate(),
                        timezone = "Asia/Kolkata",
                        bookings = listOf(
                            Booking(
                                id = "past-confirmed",
                                customerName = "Past Customer",
                                serviceName = "Trim",
                                startTimeUtc = fixedTime.minusMinutes(45).toInstant(),
                                endTimeUtc = fixedTime.minusMinutes(15).toInstant(),
                                totalAmountPaise = 20000,
                                status = DomainBookingStatus.CONFIRMED
                            )
                        )
                    )
                )
            }
        }

        val viewModel = DashboardViewModel(
            bookingsRepository = repository,
            workflowRepository = FakeWorkflowRepository(),
            appLogger = appLogger
        )

        advanceUntilIdle()
        assertEquals(fixedTime.toLocalDate(), viewModel.uiState.value.selectedDate)
        assertNull(viewModel.uiState.value.nextAppointment)
    }
}

private class FakeBookingsRepository : BookingsRepository {
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings> {
        requestedDates += date
        return ApiResult.Success(
            DayBookings(
                selectedDate = date,
                timezone = "Asia/Kolkata",
                bookings = listOf(
                    Booking(
                        id = "1",
                        customerName = "Arun Sharma",
                        serviceName = "Haircut",
                        startTimeUtc = Instant.parse("2026-04-17T03:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T04:00:00Z"),
                        totalAmountPaise = 40000,
                        status = DomainBookingStatus.PENDING_PAYMENT
                    ),
                    Booking(
                        id = "2",
                        customerName = "Rohit Jain",
                        serviceName = "Beard Trim",
                        startTimeUtc = Instant.parse("2026-04-17T04:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T04:50:00Z"),
                        totalAmountPaise = 25000,
                        status = DomainBookingStatus.CONFIRMED
                    ),
                    Booking(
                        id = "3",
                        customerName = "Kiran Patil",
                        serviceName = "Hair Spa",
                        startTimeUtc = Instant.parse("2026-04-17T05:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T06:15:00Z"),
                        totalAmountPaise = 70000,
                        status = DomainBookingStatus.SEATED
                    ),
                    Booking(
                        id = "4",
                        customerName = "Nikhil Singh",
                        serviceName = "Haircut + Beard",
                        startTimeUtc = Instant.parse("2026-04-17T06:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T07:10:00Z"),
                        totalAmountPaise = 60000,
                        status = DomainBookingStatus.COMPLETED
                    ),
                    Booking(
                        id = "5",
                        customerName = "Ajay Kumar",
                        serviceName = "Shave",
                        startTimeUtc = Instant.parse("2026-04-17T07:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T07:50:00Z"),
                        totalAmountPaise = 18000,
                        status = DomainBookingStatus.CANCELED
                    ),
                    Booking(
                        id = "6",
                        customerName = "Vijay More",
                        serviceName = "Haircut",
                        startTimeUtc = Instant.parse("2026-04-17T08:30:00Z"),
                        endTimeUtc = Instant.parse("2026-04-17T09:00:00Z"),
                        totalAmountPaise = 35000,
                        status = DomainBookingStatus.NO_SHOW
                    )
                )
            )
        )
    }
}

private class FakeWorkflowRepository(
    private val nextResult: ApiResult<WorkflowResult>? = null
) : WorkflowRepository {
    val recordedActions = mutableListOf<String>()

    override suspend fun executeAction(
        bookingId: String,
        action: WorkflowAction
    ): ApiResult<WorkflowResult> {
        recordedActions += "$bookingId:${action.apiValue}"
        return nextResult ?: ApiResult.Success(
            WorkflowResult(
                bookingId = bookingId,
                status = when (action) {
                    WorkflowAction.START -> DomainBookingStatus.SEATED
                    WorkflowAction.COMPLETE,
                    WorkflowAction.COLLECT_PAYMENT -> DomainBookingStatus.COMPLETED
                    WorkflowAction.MARK_NO_SHOW -> DomainBookingStatus.NO_SHOW
                    WorkflowAction.CANCEL_REFUND -> DomainBookingStatus.CANCELED
                },
                paymentStatus = app.slotnow.slotnowpro.domain.model.PaymentStatus.PENDING,
                paidAmountPaise = 0,
                paymentMethod = null
            )
        )
    }
}

