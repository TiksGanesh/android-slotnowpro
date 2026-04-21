package app.slotnow.slotnowpro.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.slotnow.slotnowpro.R
import app.slotnow.slotnowpro.ui.theme.SlotNowProTheme
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.actionErrorMessage) {
        val message = uiState.actionErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearActionError()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResumeRecalculateNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DashboardScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPreviousDayClick = viewModel::goToPreviousDay,
        onNextDayClick = viewModel::goToNextDay,
        onDateSelected = viewModel::selectDate,
        onTabSelected = viewModel::selectTab,
        onAppointmentsFilterSelected = viewModel::selectAppointmentsFilter,
        onStartRequested = viewModel::onStartRequested,
        onMarkDoneRequested = viewModel::onMarkDoneRequested,
        onCancelRequested = viewModel::onCancelRequested,
        onAddWalkInClick = viewModel::onAddWalkInRequested,
        onBlockTimeClick = viewModel::onBlockTimeRequested,
        onMarkBreakClick = viewModel::onMarkBreakRequested
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun DashboardScreen(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTabSelected: (BookingTab) -> Unit,
    onAppointmentsFilterSelected: (BookingFilter) -> Unit,
    onStartRequested: (DashboardBooking) -> Unit,
    onMarkDoneRequested: (DashboardBooking) -> Unit,
    onCancelRequested: (DashboardBooking) -> Unit,
    onAddWalkInClick: () -> Unit,
    onBlockTimeClick: () -> Unit,
    onMarkBreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showFabActions by remember { mutableStateOf(false) }
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                DashboardActionsFab(
                    expanded = showFabActions,
                    onExpandedChange = { showFabActions = it },
                    onAddWalkInClick = onAddWalkInClick,
                    onBlockTimeClick = onBlockTimeClick,
                    onMarkBreakClick = onMarkBreakClick
                )
            },
            bottomBar = { DashboardBottomNavigation() }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .pointerInput(uiState.selectedDate, uiState.isDateChangeLoading) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                if (uiState.isDateChangeLoading) {
                                    dragOffset = 0f
                                    return@detectHorizontalDragGestures
                                }
                                when {
                                    dragOffset <= -swipeThresholdPx -> onNextDayClick()
                                    dragOffset >= swipeThresholdPx -> onPreviousDayClick()
                                }
                                dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardHeader(
                    selectedDate = uiState.selectedDate,
                    onPreviousDayClick = onPreviousDayClick,
                    onNextDayClick = onNextDayClick,
                    onDateClick = { showDatePicker = true }
                )

                SummaryStrip(
                    totalBookings = uiState.totalBookingsCount,
                    totalRevenuePaise = uiState.totalRevenuePaise,
                    pendingBookings = uiState.pendingBookingsCount
                )

                BookingTabs(
                    selectedTab = uiState.selectedTab,
                    upcomingCount = uiState.upcomingCount,
                    appointmentsCount = uiState.appointmentsCount,
                    showUpcomingTab = !uiState.isPastDateSelected,
                    onTabSelected = onTabSelected
                )

                if (uiState.selectedTab == BookingTab.APPOINTMENTS) {
                    AppointmentsFilterRow(
                        selectedFilters = uiState.selectedAppointmentFilters,
                        onFilterSelected = onAppointmentsFilterSelected
                    )
                }

                if (uiState.nextAppointment != null) {
                    Text(
                        text = stringResource(R.string.dashboard_next_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    NextAppointmentCard(
                        booking = uiState.nextAppointment,
                        inProgressBookingId = uiState.inProgressBookingId,
                        actionInFlightBookingId = uiState.actionInFlightBookingId,
                        readOnly = uiState.isPastDateSelected,
                        onStartRequested = onStartRequested,
                        onMarkDoneRequested = onMarkDoneRequested,
                        onCancelRequested = onCancelRequested
                    )
                }


                AnimatedContent(
                    targetState = uiState.selectedTab to uiState.selectedDate,
                    modifier = Modifier.weight(1f),
                    label = "booking_list_transition"
                ) { (selectedTab, selectedDate) ->
                    if (uiState.isDateChangeLoading) {
                        BookingListSkeleton(modifier = Modifier.fillMaxSize())
                    } else {
                        BookingList(
                            selectedTab = selectedTab,
                            selectedDate = selectedDate,
                            bookings = uiState.remainingBookings,
                            inProgressBookingId = uiState.inProgressBookingId,
                            actionInFlightBookingId = uiState.actionInFlightBookingId,
                            readOnly = uiState.isPastDateSelected,
                            onStartRequested = onStartRequested,
                            onMarkDoneRequested = onMarkDoneRequested,
                            onCancelRequested = onCancelRequested,
                            onAddWalkInClick = onAddWalkInClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.toUtcStartOfDayEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE") { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE") {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millis.toLocalDateAtUtc())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.dashboard_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE") { showDatePicker = false }) {
                    Text(text = stringResource(R.string.dashboard_date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DashboardHeader(
    selectedDate: LocalDate,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()) }
    val fallbackFormatter =
        remember { DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.getDefault()) }
    val titleText = if (selectedDate == today) {
        stringResource(R.string.dashboard_header_today, selectedDate.format(dateFormatter))
    } else {
        selectedDate.format(fallbackFormatter)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.dashboard_previous_day)
            )
        }
        TextButton(onClick = onDateClick) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onNextDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.dashboard_next_day)
            )
        }
    }
}

@Composable
private fun SummaryStrip(
    totalBookings: Int,
    totalRevenuePaise: Int,
    pendingBookings: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(
            label = stringResource(R.string.dashboard_summary_total_bookings),
            value = totalBookings.toString(),
            modifier = Modifier.weight(1f)
        )
        SummaryPill(
            label = stringResource(R.string.dashboard_summary_total_revenue),
            value = formatPaiseToInr(totalRevenuePaise),
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryPill(
            label = stringResource(R.string.dashboard_summary_pending),
            value = pendingBookings.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookingTabs(
    selectedTab: BookingTab,
    upcomingCount: Int,
    appointmentsCount: Int,
    showUpcomingTab: Boolean,
    onTabSelected: (BookingTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        if (showUpcomingTab) {
            add(
                BookingTabItem(
                    BookingTab.UPCOMING,
                    stringResource(R.string.dashboard_tab_upcoming),
                    upcomingCount
                )
            )
        }
        add(
        BookingTabItem(
            BookingTab.APPOINTMENTS,
            stringResource(R.string.dashboard_tab_appointments),
            appointmentsCount
        )
        )
    }

    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)

    @Suppress("DEPRECATION")
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent
    ) {
        tabs.forEachIndexed { index, item ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabSelected(item.tab) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.dashboard_tab_with_count,
                            item.label,
                            item.count
                        ),
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun AppointmentsFilterRow(
    selectedFilters: Set<BookingFilter>,
    onFilterSelected: (BookingFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BookingFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter in selectedFilters,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label()) }
            )
        }
    }
}

@Composable
private fun NextAppointmentCard(
    booking: DashboardBooking?,
    inProgressBookingId: String?,
    actionInFlightBookingId: String?,
    readOnly: Boolean,
    onStartRequested: (DashboardBooking) -> Unit,
    onMarkDoneRequested: (DashboardBooking) -> Unit,
    onCancelRequested: (DashboardBooking) -> Unit,
    modifier: Modifier = Modifier
) {
    if (booking == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(
                text = stringResource(R.string.dashboard_next_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isInProgress = booking.id == inProgressBookingId
    val cardBorderColor =
        if (isInProgress) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, cardBorderColor, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.timeRange,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusIndicator(status = booking.status)
            }

            Text(
                text = booking.customerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = booking.serviceWithDurationLabel(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = booking.paymentBreakdownLabel(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            ActionButtons(
                booking = booking,
                isActionInFlight = booking.id == actionInFlightBookingId,
                onStartRequested = onStartRequested,
                onMarkDoneRequested = onMarkDoneRequested,
                onCancelRequested = onCancelRequested,
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth(),
                isPrimaryCard = true
            )
        }
    }
}

@Composable
private fun BookingList(
    selectedTab: BookingTab,
    selectedDate: LocalDate,
    bookings: List<DashboardBooking>,
    inProgressBookingId: String?,
    actionInFlightBookingId: String?,
    readOnly: Boolean,
    onStartRequested: (DashboardBooking) -> Unit,
    onMarkDoneRequested: (DashboardBooking) -> Unit,
    onCancelRequested: (DashboardBooking) -> Unit,
    onAddWalkInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bookings.isEmpty()) {
        EmptyState(
            isToday = selectedDate == LocalDate.now(),
            onAddWalkInClick = onAddWalkInClick,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = bookings.groupBy { booking -> booking.listSection() }
        sectionOrderForTab(selectedTab).forEach { section ->
            val sectionBookings = grouped[section].orEmpty()
            if (sectionBookings.isNotEmpty()) {
                item(key = "header_${section.name}") {
                    Text(
                        text = section.label(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(sectionBookings, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        isInProgress = booking.id == inProgressBookingId,
                        isActionInFlight = booking.id == actionInFlightBookingId,
                        readOnly = readOnly,
                        onStartRequested = onStartRequested,
                        onMarkDoneRequested = onMarkDoneRequested,
                        onCancelRequested = onCancelRequested
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingListSkeleton(modifier: Modifier = Modifier) {
    val pulseTransition = rememberInfiniteTransition(label = "dashboard_skeleton_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboard_skeleton_alpha"
    )
    val blockColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = pulseAlpha)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(4) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBlock(
                        color = blockColor,
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(16.dp)
                    )
                    SkeletonBlock(
                        color = blockColor,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(20.dp)
                    )
                    SkeletonBlock(
                        color = blockColor,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                    )
                    SkeletonBlock(
                        color = blockColor,
                        modifier = Modifier
                            .fillMaxWidth(0.25f)
                            .height(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(
                            color = blockColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        )
                        if (index % 2 == 0) {
                            SkeletonBlock(
                                color = blockColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            color = color,
            shape = MaterialTheme.shapes.medium
        )
    )
}

@Composable
private fun BookingCard(
    booking: DashboardBooking,
    isInProgress: Boolean,
    isActionInFlight: Boolean,
    readOnly: Boolean,
    onStartRequested: (DashboardBooking) -> Unit,
    onMarkDoneRequested: (DashboardBooking) -> Unit,
    onCancelRequested: (DashboardBooking) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isInProgress) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.timeRange,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                StatusIndicator(status = booking.status)
            }

            Text(
                text = booking.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = booking.serviceWithDurationLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = booking.paymentBreakdownLabel(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            // Moved Buttons below the price and gave them full width
            ActionButtons(
                booking = booking,
                isActionInFlight = isActionInFlight,
                onStartRequested = onStartRequested,
                onMarkDoneRequested = onMarkDoneRequested,
                onCancelRequested = onCancelRequested,
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth(),
                isPrimaryCard = true // Treat these like the main card buttons so they stretch
            )
        }
    }
}

@Composable
private fun ActionButtons(
    booking: DashboardBooking,
    isActionInFlight: Boolean,
    onStartRequested: (DashboardBooking) -> Unit,
    onMarkDoneRequested: (DashboardBooking) -> Unit,
    onCancelRequested: (DashboardBooking) -> Unit,
    readOnly: Boolean,
    compact: Boolean = false,
    isPrimaryCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (readOnly) {
        return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        val weightModifier = if (isPrimaryCard) Modifier.weight(1f) else Modifier

        when (booking.status) {
            // State 1: Before they arrive
            BookingStatus.CONFIRMED -> {
                Button(
                    onClick = { onStartRequested(booking) },
                    enabled = !isActionInFlight,
                    modifier = weightModifier
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_action_start),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { onCancelRequested(booking) },
                    enabled = !isActionInFlight,
                    modifier = weightModifier
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_action_cancel),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // State 2: Service is happening (In Chair)
            BookingStatus.SEATED -> {
                Button(
                    onClick = { onMarkDoneRequested(booking) },
                    enabled = !isActionInFlight,
                    modifier = weightModifier
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_action_complete),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                // Optional: You can remove Cancel here if they can't cancel mid-haircut
                TextButton(
                    onClick = { onCancelRequested(booking) },
                    enabled = !isActionInFlight,
                    modifier = weightModifier
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_action_cancel),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // State 3: Service is done, waiting for money
            BookingStatus.PENDING_PAYMENT -> {
                Button(
                    onClick = { onMarkDoneRequested(booking) },
                    enabled = !isActionInFlight,
                    modifier = weightModifier
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_action_start),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Terminal States: No actions needed
            BookingStatus.COMPLETED,
            BookingStatus.CANCELED,
            BookingStatus.NO_SHOW,
            BookingStatus.UNKNOWN -> {
                // Render nothing. The workflow is finished.
            }
        }
    }
}

@Composable
private fun DashboardActionsFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddWalkInClick: () -> Unit,
    onBlockTimeClick: () -> Unit,
    onMarkBreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.dashboard_quick_actions)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dashboard_quick_action_add_walk_in)) },
                onClick = {
                    onExpandedChange(false)
                    onAddWalkInClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dashboard_quick_action_block_time)) },
                onClick = {
                    onExpandedChange(false)
                    onBlockTimeClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dashboard_quick_action_mark_break)) },
                onClick = {
                    onExpandedChange(false)
                    onMarkBreakClick()
                }
            )
        }
    }
}

@Composable
private fun StatusIndicator(status: BookingStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = status.indicatorColor(), shape = MaterialTheme.shapes.small)
        )
        Text(
            text = status.toLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    isToday: Boolean,
    onAddWalkInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[]",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isToday) {
                stringResource(R.string.dashboard_empty_state_today)
            } else {
                stringResource(R.string.dashboard_empty_state_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(onClick = onAddWalkInClick) {
            Text(text = stringResource(R.string.dashboard_empty_state_add_walk_in))
        }
    }
}

@Composable
private fun BookingSection.label(): String {
    return when (this) {
        BookingSection.UPCOMING -> stringResource(R.string.dashboard_section_upcoming)
        BookingSection.COMPLETED -> stringResource(R.string.dashboard_section_completed)
        BookingSection.CANCELLED -> stringResource(R.string.dashboard_section_cancelled)
        BookingSection.NO_SHOW -> stringResource(R.string.dashboard_section_no_show)
    }
}

private fun DashboardBooking.listSection(): BookingSection {
    return when (status) {
        BookingStatus.COMPLETED -> BookingSection.COMPLETED
        BookingStatus.CANCELED -> BookingSection.CANCELLED
        BookingStatus.NO_SHOW -> BookingSection.NO_SHOW

        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.SEATED,
        BookingStatus.UNKNOWN -> BookingSection.UPCOMING
    }
}

private enum class BookingSection {
    UPCOMING,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

private fun sectionOrderForTab(selectedTab: BookingTab): List<BookingSection> {
    return when (selectedTab) {
        BookingTab.UPCOMING -> listOf(BookingSection.UPCOMING)
        BookingTab.APPOINTMENTS -> listOf(
            BookingSection.COMPLETED,
            BookingSection.CANCELLED,
            BookingSection.NO_SHOW
        )
    }
}

@Composable
private fun BookingFilter.label(): String {
    return when (this) {
        BookingFilter.COMPLETED -> stringResource(R.string.dashboard_filter_completed)
        BookingFilter.CANCELLED -> stringResource(R.string.dashboard_filter_cancelled)
        BookingFilter.NO_SHOW -> stringResource(R.string.dashboard_filter_no_show)
    }
}

@Composable
private fun DashboardBooking.serviceWithDurationLabel(): String {
    val serviceLabel = serviceName ?: stringResource(R.string.dashboard_service_walk_in)
    if (!hasScheduleDuration()) {
        return serviceLabel
    }
    return stringResource(
        R.string.dashboard_service_with_duration,
        serviceLabel,
        serviceDurationMinutes()
    )
}

@Composable
private fun DashboardBooking.paymentBreakdownLabel(): String {
    return stringResource(
        R.string.dashboard_amount_advance_pending,
        formatPaiseToInrCompact(amountPaise),
        formatPaiseToInrFixed(pendingAmountPaise)
    )
}

private fun DashboardBooking.hasScheduleDuration(): Boolean {
    return when (status) {
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.SEATED -> true

        BookingStatus.COMPLETED,
        BookingStatus.CANCELED,
        BookingStatus.NO_SHOW,
        BookingStatus.UNKNOWN -> false
    }
}

private fun DashboardBooking.serviceDurationMinutes(): Int {
    return Duration.between(startTime, endTime)
        .toMinutes()
        .coerceAtLeast(0)
        .toInt()
}

@Composable
private fun BookingStatus.toLabel(): String {
    return when (this) {
        BookingStatus.PENDING_PAYMENT -> stringResource(R.string.dashboard_status_pending_payment)
        BookingStatus.CONFIRMED -> stringResource(R.string.dashboard_status_confirmed)
        BookingStatus.SEATED -> stringResource(R.string.dashboard_status_seated)
        BookingStatus.COMPLETED -> stringResource(R.string.dashboard_status_completed)
        BookingStatus.CANCELED -> stringResource(R.string.dashboard_status_cancelled)
        BookingStatus.NO_SHOW -> stringResource(R.string.dashboard_status_no_show)
        BookingStatus.UNKNOWN -> stringResource(R.string.dashboard_status_unknown)
    }
}

@Composable
private fun BookingStatus.indicatorColor(): Color {
    return when (this) {
        BookingStatus.COMPLETED -> Color(0xFF1B9E3E)
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.SEATED -> Color(0xFFE6A700)

        BookingStatus.CANCELED,
        BookingStatus.NO_SHOW -> Color(0xFFD32F2F)

        BookingStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
}

private fun BookingStatus.primaryActionLabelRes(): Int? {
    return when (this) {
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED -> R.string.dashboard_action_start

        BookingStatus.SEATED -> R.string.dashboard_action_complete
        BookingStatus.COMPLETED,
        BookingStatus.CANCELED,
        BookingStatus.NO_SHOW,
        BookingStatus.UNKNOWN -> null
    }
}

private data class BookingTabItem(
    val tab: BookingTab,
    val label: String,
    val count: Int
)

private fun formatPaiseToInr(paise: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return format.format(paise / 100.0)
}

private fun formatPaiseToInrCompact(paise: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 2
    return format.format(paise / 100.0)
}

private fun formatPaiseToInrFixed(paise: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return format.format(paise / 100.0)
}

private fun LocalDate.toUtcStartOfDayEpochMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateAtUtc(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
}

@Composable
private fun DashboardBottomNavigation(modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = stringResource(R.string.dashboard_nav_dashboard_short)
                )
            },
            label = { Text(text = stringResource(R.string.dashboard_nav_dashboard)) }
        )
        NavigationBarItem(
            selected = false,
            enabled = false,
            onClick = {},
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.dashboard_nav_profile_short)
                )
            },
            label = { Text(text = stringResource(R.string.dashboard_nav_profile)) }
        )
        NavigationBarItem(
            selected = false,
            enabled = false,
            onClick = {},
            icon = {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.dashboard_nav_more_short)
                )
            },
            label = { Text(text = stringResource(R.string.dashboard_nav_more)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    SlotNowProTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                selectedDate = LocalDate.of(2026, 4, 17),
                selectedTab = BookingTab.UPCOMING,
                allBookings = emptyList(),
                filteredBookings = emptyList(),
                remainingBookings = listOf(
                    DashboardBooking(
                        id = "2",
                        customerName = "Rohit Jain",
                        serviceName = "Beard Trim",
                        timeRange = "10:00 - 10:30",
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(10, 30),
                        amountPaise = 30000,
                        status = BookingStatus.CONFIRMED
                    )
                ),
                nextAppointment = DashboardBooking(
                    id = "1",
                    customerName = "Arun Sharma",
                    serviceName = "Haircut",
                    timeRange = "09:30 - 10:00",
                    startTime = LocalTime.of(9, 30),
                    endTime = LocalTime.of(10, 0),
                    amountPaise = 40000,
                    status = BookingStatus.SEATED
                ),
                upcomingCount = 4,
                cancelledCount = 1,
                noShowCount = 1,
                filteredBookingCount = 4,
                totalBookingsCount = 6,
                pendingBookingsCount = 2,
                totalRevenuePaise = 250000,
                inProgressBookingId = "1"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onPreviousDayClick = {},
            onNextDayClick = {},
            onDateSelected = {},
            onTabSelected = {},
            onAppointmentsFilterSelected = {},
            onStartRequested = {},
            onMarkDoneRequested = {},
            onCancelRequested = {},
            onAddWalkInClick = {},
            onBlockTimeClick = {},
            onMarkBreakClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenLoadingPreview() {
    SlotNowProTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                selectedDate = LocalDate.of(2026, 4, 17),
                selectedTab = BookingTab.UPCOMING,
                allBookings = emptyList(),
                isDateChangeLoading = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onPreviousDayClick = {},
            onNextDayClick = {},
            onDateSelected = {},
            onTabSelected = {},
            onAppointmentsFilterSelected = {},
            onStartRequested = {},
            onMarkDoneRequested = {},
            onCancelRequested = {},
            onAddWalkInClick = {},
            onBlockTimeClick = {},
            onMarkBreakClick = {}
        )
    }
}
