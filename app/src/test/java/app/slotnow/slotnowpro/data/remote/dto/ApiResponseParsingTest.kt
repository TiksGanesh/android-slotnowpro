package app.slotnow.slotnowpro.data.remote.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseParsingTest {

    private val gson = Gson()

    @Test
    fun `parses success envelope with data and meta`() {
        val json = """
            {
              "success": true,
              "data": {
                "maskedPhone": "+91 98xxx x3210",
                "otpExpiresInSeconds": 300
              },
              "meta": {
                "timestamp": "2026-04-13T10:30:00Z",
                "timezone": "Asia/Kolkata"
              }
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponse<RequestOtpDataStub>>() {}.type
        val response: ApiResponse<RequestOtpDataStub> = gson.fromJson(json, type)

        assertTrue(response.success)
        assertEquals("+91 98xxx x3210", response.data?.maskedPhone)
        assertEquals(300, response.data?.otpExpiresInSeconds)
        assertEquals("Asia/Kolkata", response.meta?.timezone)
        assertNull(response.error)
    }

    @Test
    fun `parses error envelope with code message and hint`() {
        val json = """
            {
              "success": false,
              "error": {
                "code": "SHOP_NOT_FOUND",
                "message": "No shop matches this slug",
                "hint": "Check with your manager"
              }
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponse<Any>>() {}.type
        val response: ApiResponse<Any> = gson.fromJson(json, type)

        assertFalse(response.success)
        assertNull(response.data)
        assertNotNull(response.error)
        assertEquals("SHOP_NOT_FOUND", response.error?.code)
        assertEquals("No shop matches this slug", response.error?.message)
        assertEquals("Check with your manager", response.error?.hint)
    }

    @Test
    fun `parses response when meta timezone is omitted`() {
        val json = """
            {
              "success": true,
              "data": {
                "maskedPhone": "+91 98xxx x3210",
                "otpExpiresInSeconds": 300
              },
              "meta": {
                "timestamp": "2026-04-13T10:30:00Z"
              }
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponse<RequestOtpDataStub>>() {}.type
        val response: ApiResponse<RequestOtpDataStub> = gson.fromJson(json, type)

        assertTrue(response.success)
        assertEquals("2026-04-13T10:30:00Z", response.meta?.timestamp)
        assertNull(response.meta?.timezone)
    }

    data class RequestOtpDataStub(
        val maskedPhone: String,
        val otpExpiresInSeconds: Int
    )
}

