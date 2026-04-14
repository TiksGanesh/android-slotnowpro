package app.slotnow.slotnowpro.data.remote.api

import app.slotnow.slotnowpro.data.remote.dto.ApiResponse
import app.slotnow.slotnowpro.data.remote.dto.dashboard.BookingsListData
import app.slotnow.slotnowpro.data.remote.dto.dashboard.WorkflowData
import app.slotnow.slotnowpro.data.remote.dto.dashboard.WorkflowRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Dashboard API endpoints - all require Bearer token authentication.
 */
interface BarberDashboardApi {
    @GET("dashboard/bookings")
    suspend fun getBookings(
        @Query("date") date: String? = null
    ): ApiResponse<BookingsListData>

    @POST("dashboard/bookings/{bookingId}/workflow")
    suspend fun executeWorkflow(
        @Path("bookingId") bookingId: String,
        @Body request: WorkflowRequest
    ): ApiResponse<WorkflowData>
}


