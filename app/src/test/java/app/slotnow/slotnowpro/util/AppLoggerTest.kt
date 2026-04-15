package app.slotnow.slotnowpro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLoggerTest {

    private val appLogger = AppLogger()

    @Test
    fun `loggerIIfDebug logs when debug true`() {
        val sink = RecordingLogSink()

        appLogger.logInfoDebug(
            tag = "Onboarding",
            message = "Shop validation started",
            isDebug = true,
            sink = sink
        )

        assertEquals(1, sink.infoCalls.size)
        assertEquals(Triple("Onboarding", "Shop validation started", null), sink.infoCalls.first())
    }

    @Test
    fun `loggerIIfDebug does not log when debug false`() {
        val sink = RecordingLogSink()

        appLogger.logInfoDebug(
            tag = "Onboarding",
            message = "Shop validation started",
            isDebug = false,
            sink = sink
        )

        assertEquals(0, sink.infoCalls.size)
        assertEquals(0, sink.errorCalls.size)
    }

    @Test
    fun `loggerEIfDebug logs throwable when debug true`() {
        val sink = RecordingLogSink()
        val throwable = IllegalStateException("boom")

        appLogger.logErrorDebug(
            tag = "Onboarding",
            message = "Shop validation failed",
            throwable = throwable,
            isDebug = true,
            sink = sink
        )

        assertEquals(1, sink.errorCalls.size)
        val call = sink.errorCalls.first()
        assertEquals("Onboarding", call.first)
        assertEquals("Shop validation failed", call.second)
        assertEquals(throwable, call.third)
    }

    @Test
    fun `loggerEIfDebug does not log when debug false`() {
        val sink = RecordingLogSink()

        appLogger.logErrorDebug(
            tag = "Onboarding",
            message = "Shop validation failed",
            throwable = null,
            isDebug = false,
            sink = sink
        )

        assertEquals(0, sink.infoCalls.size)
        assertEquals(0, sink.errorCalls.size)
    }

    private class RecordingLogSink : LogSink {
        val infoCalls = mutableListOf<Triple<String, String, Throwable?>>()
        val errorCalls = mutableListOf<Triple<String, String, Throwable?>>()
        val warnCalls = mutableListOf<Triple<String, String, Throwable?>>()

        override fun info(tag: String, message: String) {
            infoCalls.add(Triple(tag, message, null))
        }

        override fun error(tag: String, message: String, throwable: Throwable?) {
            errorCalls.add(Triple(tag, message, throwable))
        }

        override fun warn(tag: String, message: String, throwable: Throwable?) {
            warnCalls.add(Triple(tag, message, throwable))
        }
    }
}

