package com.example.konteksto

import com.example.konteksto.ui.host.buildQrApiUrl
import com.example.konteksto.ui.host.qrFilenameForTalkCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentQrTest {

    @Test
    fun testBuildQrApiUrl() {
        val url = buildQrApiUrl("RUMNXJ")
        assertEquals("https://api.qrserver.com/v1/create-qr-code/?size=450x450&data=RUMNXJ", url)
    }

    @Test
    fun testQrFilenameStandardCode() {
        val filename = qrFilenameForTalkCode("RUMNXJ")
        assertEquals("konteksto_qr_RUMNXJ.png", filename)
    }

    @Test
    fun testQrFilenameSanitization() {
        val filename = qrFilenameForTalkCode("TALK-01#")
        assertEquals("konteksto_qr_TALK01.png", filename)
    }

    @Test
    fun testQrFilenameFallbackWhenBlank() {
        val filename = qrFilenameForTalkCode("   ")
        assertEquals("konteksto_qr_code.png", filename)
    }

    @Test
    fun testQrFilenameHasPngExtension() {
        val filename = qrFilenameForTalkCode("2MFA5X")
        assertTrue(filename.endsWith(".png"))
        assertTrue(filename.startsWith("konteksto_qr_"))
    }
}
