package app.slotnow.slotnowpro.data.repository

import app.slotnow.slotnowpro.data.remote.api.BarberDashboardApi
import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.dashboard.WorkflowData
import app.slotnow.slotnowpro.domain.model.BookingStatus
import app.slotnow.slotnowpro.domain.model.WorkflowAction
import app.slotnow.slotnowpro.util.ApiResult
import app.slotnow.slotnowpro.util.AppLogger
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowRepositoryImplTest {

    private val api = mockk<BarberDashboardApi>()
    private val gson = Gson()
    private val appLogger = mockk<AppLogger>(relaxed = true)

    @Test
    fun `maps workflow response into domain model`() = runTest {
        coEvery {
            api.executeWorkflow(
                bookingId = "booking-1",
                request = any()
            )
        } returns ApiResponse(
            success = true,
            data = WorkflowData(
                bookingId = "booking-1",
                status = "seated",
                paymentStatus = "pending",
                paidAmount = 0,
                paymentMethod = null
            )
        )

        val repository = WorkflowRepositoryImpl(
            dashboardApi = api,
            gson = gson,
            appLogger = appLogger
        )

        val result = repository.executeAction(
            bookingId = "booking-1",
            action = WorkflowAction.START
        )

        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success
        assertEquals("booking-1", success.data.bookingId)
        assertEquals(BookingStatus.SEATED, success.data.status)
    }

    @Test
    fun `returns api error when envelope is unsuccessful`() = runTest {
        coEvery {
            api.executeWorkflow(
                bookingId = "booking-1",
                request = any()
            )
        } returns ApiResponse(
            success = false,
            data = null,
            error = app.slotnow.slotnowpro.data.remote.dto.ApiErrorBody(
                code = "INVALID_TRANSITION",
                message = "Cannot start this appointment",
                hint = null
            )
        )

        val repository = WorkflowRepositoryImpl(
            dashboardApi = api,
            gson = gson,
            appLogger = appLogger
        )

        val result = repository.executeAction(
            bookingId = "booking-1",
            action = WorkflowAction.START
        )

        assertTrue(result is ApiResult.ApiError)
        val error = result as ApiResult.ApiError
        assertEquals("INVALID_TRANSITION", error.code)
    }
}

