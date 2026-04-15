package app.slotnow.slotnowpro.presentation

import androidx.lifecycle.ViewModel
import app.slotnow.slotnowpro.util.AppLogger

abstract class BaseViewModel(
    private val appLogger: AppLogger
) : ViewModel() {

    protected fun logInfoDebug(
        tag: String,
        message: String
    ) {
        appLogger.logInfoDebug(tag = tag, message = message)
    }

    protected fun logErrorDebug(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        appLogger.logErrorDebug(tag = tag, message = message, throwable = throwable)
    }

    protected fun logWarnDebug(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        appLogger.logWarningDebug(tag = tag, message = message, throwable = throwable)
    }
}

