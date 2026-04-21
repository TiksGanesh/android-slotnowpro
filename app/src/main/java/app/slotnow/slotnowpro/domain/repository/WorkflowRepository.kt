package app.slotnow.slotnowpro.domain.repository

import app.slotnow.slotnowpro.domain.model.WorkflowAction
import app.slotnow.slotnowpro.domain.model.WorkflowResult
import app.slotnow.slotnowpro.util.ApiResult

interface WorkflowRepository {
    suspend fun executeAction(
        bookingId: String,
        action: WorkflowAction
    ): ApiResult<WorkflowResult>
}

