package app.slotnow.slotnowpro.domain.model

enum class WorkflowAction(val apiValue: String) {
    START(apiValue = "start"),
    COMPLETE(apiValue = "complete"),
    COLLECT_PAYMENT(apiValue = "collect_payment"),
    MARK_NO_SHOW(apiValue = "mark_no_show"),
    CANCEL_REFUND(apiValue = "cancel_refund")
}

