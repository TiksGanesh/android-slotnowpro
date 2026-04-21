package app.slotnow.slotnowpro.presentation.dashboard

import androidx.lifecycle.viewModelScope
import app.slotnow.slotnowpro.domain.model.Booking
import app.slotnow.slotnowpro.domain.model.WorkflowAction
import app.slotnow.slotnowpro.domain.repository.BookingsRepository
import app.slotnow.slotnowpro.domain.repository.WorkflowRepository
import app.slotnow.slotnowpro.presentation.BaseViewModel
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import app.slotnow.slotnowpro.domain.model.BookingStatus as DomainBookingStatus

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val workflowRepository: WorkflowRepository,
    appLogger: AppLogger
) : BaseViewModel(appLogger) {

    private companion object {
        private const val LOG_TAG = "DashboardViewModel"
    }

    private var loadBookingsJob: Job? = null
    private var currentLoadRequestToken = 0

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            selectedDate = LocalDate.now(),
            selectedTab = BookingTab.UPCOMING,
            allBookings = emptyList()
        ).withDerivedState()
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadBookingsForDate(_uiState.value.selectedDate)
    }

    fun goToPreviousDay() {
        if (_uiState.value.isDateChangeLoading) {
            return
        }
        val nextDate = _uiState.value.selectedDate.minusDays(1)
        _uiState.update { state ->
            state.copy(
                selectedDate = nextDate,
                currentTime = currentLocalTimeForSelectedDate(nextDate, state.shopTimezone)
            ).withDerivedState()
        }
        loadBookingsForDate(nextDate, showDateNavigationLoading = true)
    }

    fun goToNextDay() {
        if (_uiState.value.isDateChangeLoading) {
            return
        }
        val nextDate = _uiState.value.selectedDate.plusDays(1)
        _uiState.update { state ->
            state.copy(
                selectedDate = nextDate,
                currentTime = currentLocalTimeForSelectedDate(nextDate, state.shopTimezone)
            ).withDerivedState()
        }
        loadBookingsForDate(nextDate, showDateNavigationLoading = true)
    }

    fun goToToday() {
        if (_uiState.value.isDateChangeLoading) {
            return
        }
        val today = LocalDate.now()
        _uiState.update { state ->
            state.copy(
                selectedDate = today,
                currentTime = currentLocalTimeForSelectedDate(today, state.shopTimezone)
            ).withDerivedState()
        }
        loadBookingsForDate(today, showDateNavigationLoading = true)
    }

    fun selectDate(date: LocalDate) {
        if (_uiState.value.isDateChangeLoading || _uiState.value.isLoading || date == _uiState.value.selectedDate) {
            return
        }
        _uiState.update { state ->
            state.copy(
                selectedDate = date,
                currentTime = currentLocalTimeForSelectedDate(date, state.shopTimezone)
            ).withDerivedState()
        }
        loadBookingsForDate(date, showDateNavigationLoading = true)
    }

    fun selectTab(tab: BookingTab) {
        _uiState.update { state ->
            val today = getTodayInShopTimezone(state.shopTimezone)
            val coercedTab = if (state.selectedDate.isBefore(today)) {
                BookingTab.APPOINTMENTS
            } else {
                tab
            }
            state.copy(selectedTab = coercedTab).withDerivedState()
        }
    }

    fun selectAppointmentsFilter(filter: BookingFilter) {
        _uiState.update { state ->
            val updatedFilters = if (filter in state.selectedAppointmentFilters) {
                state.selectedAppointmentFilters - filter
            } else {
                state.selectedAppointmentFilters + filter
            }
            state.copy(selectedAppointmentFilters = updatedFilters).withDerivedState()
        }
    }


    fun clearAppointmentsFilters() {
        _uiState.update { state ->
            state.copy(selectedAppointmentFilters = emptySet()).withDerivedState()
        }
    }

    fun refresh() {
        loadBookingsForDate(_uiState.value.selectedDate)
    }

    fun onResumeRecalculateNow() {
        _uiState.update { state ->
            state.copy(
                currentTime = currentLocalTimeForSelectedDate(state.selectedDate, state.shopTimezone)
            ).withDerivedState()
        }
    }

    fun onStartRequested(booking: DashboardBooking) {
        if (_uiState.value.isPastDateSelected) {
            logInfoDebug(LOG_TAG, "Ignoring start action for past date booking=${booking.id}")
            return
        }
        executeWorkflowForBooking(bookingId = booking.id, action = WorkflowAction.START)
    }

    fun onMarkDoneRequested(booking: DashboardBooking) {
        if (_uiState.value.isPastDateSelected) {
            logInfoDebug(LOG_TAG, "Ignoring mark done action for past date booking=${booking.id}")
            return
        }
        val action = when (booking.status) {
            BookingStatus.SEATED -> WorkflowAction.COMPLETE
            BookingStatus.CONFIRMED,
            BookingStatus.PENDING_PAYMENT -> WorkflowAction.START

            BookingStatus.COMPLETED -> WorkflowAction.COLLECT_PAYMENT
            BookingStatus.CANCELED,
            BookingStatus.NO_SHOW,
            BookingStatus.UNKNOWN -> null
        }
        if (action == null) {
            logWarnDebug(
                LOG_TAG,
                "No workflow action mapped for booking=${booking.id} status=${booking.status}"
            )
            return
        }
        executeWorkflowForBooking(bookingId = booking.id, action = action)
    }

    fun onCancelRequested(booking: DashboardBooking) {
        if (_uiState.value.isPastDateSelected) {
            logInfoDebug(LOG_TAG, "Ignoring cancel action for past date booking=${booking.id}")
            return
        }
        executeWorkflowForBooking(bookingId = booking.id, action = WorkflowAction.CANCEL_REFUND)
    }

    fun clearActionError() {
        _uiState.update { state -> state.copy(actionErrorMessage = null).withDerivedState() }
    }

    fun onAddWalkInRequested() {
        logInfoDebug(LOG_TAG, "UI callback: add walk-in requested")
    }

    fun onBlockTimeRequested() {
        logInfoDebug(LOG_TAG, "UI callback: block time requested")
    }

    fun onMarkBreakRequested() {
        logInfoDebug(LOG_TAG, "UI callback: mark break requested")
    }

    private fun getTodayInShopTimezone(timezone: String?): LocalDate {
        val zoneId = timezone.toSafeZoneId()
        return ZonedDateTime.now(zoneId).toLocalDate()
    }

    private fun loadBookingsForDate(
        date: LocalDate,
        showDateNavigationLoading: Boolean = false
    ) {
        // Cancel previous load and increment token to ignore stale responses
        loadBookingsJob?.cancel()
        currentLoadRequestToken++
        val requestToken = currentLoadRequestToken

        loadBookingsJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    isDateChangeLoading = showDateNavigationLoading,
                    errorMessage = null,
                    actionErrorMessage = null
                ).withDerivedState()
            }

            when (val result = bookingsRepository.getBookingsForDate(date)) {
                is ApiResult.Success -> {
                    // Only apply response if this is the current request
                    if (requestToken != currentLoadRequestToken) {
                        logInfoDebug(LOG_TAG, "Ignoring stale bookings response for date=$date")
                        return@launch
                    }

                    val uiBookings = result.data.bookings.map { booking ->
                        booking.toPresentationModel(timezone = result.data.timezone)
                    }
                    _uiState.update { state ->
                        state.copy(
                            selectedDate = result.data.selectedDate,
                            shopTimezone = result.data.timezone,
                            currentTime = currentLocalTimeForSelectedDate(
                                selectedDate = result.data.selectedDate,
                                timezone = result.data.timezone
                            ),
                            allBookings = uiBookings,
                            isLoading = false,
                            isDateChangeLoading = false,
                            errorMessage = null
                        ).withDerivedState()
                    }
                }

                is ApiResult.ApiError -> {
                    // Only apply error if this is the current request
                    if (requestToken != currentLoadRequestToken) {
                        logInfoDebug(LOG_TAG, "Ignoring stale error for date=$date")
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isDateChangeLoading = false,
                            errorMessage = result.message
                        ).withDerivedState()
                    }
                    logWarnDebug(
                        LOG_TAG,
                        "Bookings API error: ${result.code} (${result.httpStatus})"
                    )
                }

                is ApiResult.NetworkError -> {
                    // Only apply error if this is the current request
                    if (requestToken != currentLoadRequestToken) {
                        logInfoDebug(LOG_TAG, "Ignoring stale network error for date=$date")
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isDateChangeLoading = false,
                            errorMessage = result.cause.message ?: "Network error"
                        ).withDerivedState()
                    }
                    logWarnDebug(LOG_TAG, "Network error while loading bookings", result.cause)
                }
            }
        }
    }

    private fun executeWorkflowForBooking(bookingId: String, action: WorkflowAction) {
        if (_uiState.value.isPastDateSelected) {
            logInfoDebug(
                LOG_TAG,
                "Ignoring workflow action=${action.apiValue} for past date booking=$bookingId"
            )
            return
        }
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(actionInFlightBookingId = bookingId, actionErrorMessage = null)
                    .withDerivedState()
            }

            when (val result =
                workflowRepository.executeAction(bookingId = bookingId, action = action)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedBookings = state.allBookings.map { booking ->
                            if (booking.id == result.data.bookingId) {
                                booking.copy(status = result.data.status.toUiStatus())
                            } else {
                                booking
                            }
                        }
                        state.copy(
                            allBookings = updatedBookings,
                            actionInFlightBookingId = null,
                            actionErrorMessage = null
                        ).withDerivedState()
                    }
                }

                is ApiResult.ApiError -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInFlightBookingId = null,
                            actionErrorMessage = result.message
                        ).withDerivedState()
                    }
                    logWarnDebug(
                        LOG_TAG,
                        "Workflow API error: ${result.code} (${result.httpStatus})"
                    )
                }

                is ApiResult.NetworkError -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInFlightBookingId = null,
                            actionErrorMessage = result.cause.message ?: "Network error"
                        ).withDerivedState()
                    }
                    logWarnDebug(LOG_TAG, "Network error while executing workflow", result.cause)
                }
            }
        }
    }
}

data class DashboardUiState(
    val selectedDate: LocalDate,
    val selectedTab: BookingTab,
    val isPastDateSelected: Boolean = false,
    val allBookings: List<DashboardBooking>,
    val selectedAppointmentFilters: Set<BookingFilter> = BookingFilter.entries.toSet(),
    val filteredBookings: List<DashboardBooking> = emptyList(),
    val upcomingCount: Int = 0,
    val appointmentsCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val noShowCount: Int = 0,
    val filteredBookingCount: Int = 0,
    val totalBookingsCount: Int = 0,
    val pendingBookingsCount: Int = 0,
    val totalRevenuePaise: Int = 0,
    val shopTimezone: String? = null,
    val currentTime: LocalTime? = null,
    val nextAppointment: DashboardBooking? = null,
    val remainingBookings: List<DashboardBooking> = emptyList(),
    val inProgressBookingId: String? = null,
    val actionInFlightBookingId: String? = null,
    val isLoading: Boolean = false,
    val isDateChangeLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null
)

enum class BookingTab {
    UPCOMING,
    APPOINTMENTS
}

enum class BookingFilter {
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

data class DashboardBooking(
    val id: String,
    val customerName: String,
    val serviceName: String?,
    val timeRange: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val amountPaise: Int,
    val paidAmountPaise: Int = 0,
    val pendingAmountPaise: Int = amountPaise,
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

private fun DashboardUiState.withDerivedState(): DashboardUiState {
    val today = getTodayInShopTimezoneForState(this.shopTimezone)
    val isSelectedToday = selectedDate == today
    val isPastDateSelected = selectedDate.isBefore(today)
    val effectiveSelectedTab = if (isPastDateSelected) BookingTab.APPOINTMENTS else selectedTab

    val normalizedBookings = allBookings.map { booking ->
        val shouldAutoNoShow = isSelectedToday &&
                currentTime != null &&
                booking.status == BookingStatus.PENDING_PAYMENT &&
                booking.endTime < currentTime
        if (shouldAutoNoShow) {
            booking.copy(status = BookingStatus.NO_SHOW)
        } else {
            booking
        }
    }

    val upcomingStatuses = setOf(
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.SEATED
    )
    val actionableStatuses = setOf(
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.SEATED
    )

    val sortedBookings = normalizedBookings.sortedBy { booking -> booking.startTime }
    val appointmentStatusesByFilter = mapOf(
        BookingFilter.COMPLETED to BookingStatus.COMPLETED,
        BookingFilter.CANCELLED to BookingStatus.CANCELED,
        BookingFilter.NO_SHOW to BookingStatus.NO_SHOW
    )
    val selectedAppointmentStatuses = selectedAppointmentFilters.mapNotNull { filter ->
        appointmentStatusesByFilter[filter]
    }.toSet()

    val filteredBookings = when (effectiveSelectedTab) {
        BookingTab.UPCOMING -> sortedBookings.filter { booking -> booking.status in upcomingStatuses }
        BookingTab.APPOINTMENTS -> sortedBookings.filter { booking ->
            booking.status in selectedAppointmentStatuses
        }
    }

    val actionableBookings =
        sortedBookings.filter { booking -> booking.status in actionableStatuses }
    val nextAppointment = when {
        effectiveSelectedTab != BookingTab.UPCOMING -> null
        selectedDate.isAfter(today) -> actionableBookings.firstOrNull()
        selectedDate.isBefore(today) -> null
        else -> currentTime?.let { now ->
            actionableBookings.firstOrNull { booking -> booking.startTime > now }
        }
    }

    val inProgressBooking = currentTime?.let { now ->
        actionableBookings.firstOrNull { booking -> booking.startTime <= now && now < booking.endTime }
    }
    val remainingBookings = filteredBookings.filter { booking -> booking.id != nextAppointment?.id }

    return copy(
        selectedTab = effectiveSelectedTab,
        isPastDateSelected = isPastDateSelected,
        filteredBookings = filteredBookings,
        upcomingCount = normalizedBookings.count { booking -> booking.status in upcomingStatuses },
        appointmentsCount = normalizedBookings.count { booking ->
            booking.status in appointmentStatusesByFilter.values
        },
        completedCount = normalizedBookings.count { booking -> booking.status == BookingStatus.COMPLETED },
        cancelledCount = normalizedBookings.count { booking -> booking.status == BookingStatus.CANCELED },
        noShowCount = normalizedBookings.count { booking -> booking.status == BookingStatus.NO_SHOW },
        filteredBookingCount = filteredBookings.size,
        totalBookingsCount = normalizedBookings.size,
        pendingBookingsCount = normalizedBookings.count { booking -> booking.status == BookingStatus.PENDING_PAYMENT },
        totalRevenuePaise = normalizedBookings.sumOf { booking -> booking.amountPaise },
        nextAppointment = nextAppointment,
        remainingBookings = remainingBookings,
        inProgressBookingId = inProgressBooking?.id
    )
}

private fun Booking.toPresentationModel(timezone: String?): DashboardBooking {
    val zoneId = timezone.toSafeZoneId()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    val localStart = startTimeUtc.atZone(zoneId).toLocalTime()
    val localEnd = endTimeUtc.atZone(zoneId).toLocalTime()

    return DashboardBooking(
        id = id,
        customerName = customerName,
        serviceName = serviceName,
        timeRange = "${localStart.format(timeFormatter)} - ${localEnd.format(timeFormatter)}",
        startTime = localStart,
        endTime = localEnd,
        amountPaise = totalAmountPaise,
        paidAmountPaise = paidAmountPaise,
        pendingAmountPaise = pendingAmountPaise,
        status = status.toUiStatus()
    )
}

private fun currentLocalTimeForSelectedDate(
    selectedDate: LocalDate,
    timezone: String?
): LocalTime? {
    val zoneId = timezone.toSafeZoneId()
    val zonedNow = ZonedDateTime.now(zoneId)
    return if (zonedNow.toLocalDate() == selectedDate) {
        zonedNow.toLocalTime()
    } else {
        null
    }
}

private fun getTodayInShopTimezoneForState(timezone: String?): LocalDate {
    val zoneId = timezone.toSafeZoneId()
    return ZonedDateTime.now(zoneId).toLocalDate()
}

private fun String?.toSafeZoneId(): ZoneId {
    if (this.isNullOrBlank()) {
        return ZoneId.systemDefault()
    }
    return try {
        ZoneId.of(this)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }
}

private fun DomainBookingStatus.toUiStatus(): BookingStatus {
    return when (this) {
        DomainBookingStatus.PENDING_PAYMENT -> BookingStatus.PENDING_PAYMENT
        DomainBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
        DomainBookingStatus.SEATED -> BookingStatus.SEATED
        DomainBookingStatus.COMPLETED -> BookingStatus.COMPLETED
        DomainBookingStatus.CANCELED -> BookingStatus.CANCELED
        DomainBookingStatus.NO_SHOW -> BookingStatus.NO_SHOW
        DomainBookingStatus.UNKNOWN -> BookingStatus.UNKNOWN
    }
}

