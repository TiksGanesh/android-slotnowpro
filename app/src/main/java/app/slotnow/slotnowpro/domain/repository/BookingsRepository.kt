package app.slotnow.slotnowpro.domain.repository

import app.slotnow.slotnowpro.domain.model.DayBookings
import app.slotnow.slotnowpro.util.ApiResult
import java.time.LocalDate

interface BookingsRepository {
    suspend fun getBookingsForDate(date: LocalDate): ApiResult<DayBookings>
}

