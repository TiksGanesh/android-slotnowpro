package app.slotnow.slotnowpro.util

import android.util.Log
import app.slotnow.slotnowpro.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-gated app logger. Logs are no-op for release builds by default.
 */
@Singleton
class AppLogger @Inject constructor() {

    fun logInfoDebug(
        tag: String,
        message: String,
        isDebug: Boolean = BuildConfig.DEBUG,
        sink: LogSink = AndroidLogSink
    ) {
        if (!isDebug) return
        sink.info(tag, message)
    }

    fun logErrorDebug(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        isDebug: Boolean = BuildConfig.DEBUG,
        sink: LogSink = AndroidLogSink
    ) {
        if (!isDebug) return
        sink.error(tag, message, throwable)
    }

    fun logWarningDebug(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        isDebug: Boolean = BuildConfig.DEBUG,
        sink: LogSink = AndroidLogSink
    ) {
        if (!isDebug) return
        sink.warn(tag, message, throwable)
    }
}

interface LogSink {
    fun info(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable?)
    fun warn(tag: String, message: String, throwable: Throwable?)
}

object AndroidLogSink : LogSink {
    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }
}

