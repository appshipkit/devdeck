package io.devdeck.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionTitleTest {

    @Test
    fun `splits a trailing emoji from the name`() {
        assertEquals("📚" to "Logs", splitSectionTitle("Logs 📚"))
        assertEquals("ℹ️" to "Info", splitSectionTitle("Info ℹ️"))
        assertEquals("⚡" to "Performance", splitSectionTitle("Performance ⚡"))
        assertEquals("🛠️" to "App", splitSectionTitle("App 🛠️"))
    }

    @Test
    fun `splits a leading emoji from the name`() {
        assertEquals("🧪" to "Experiments", splitSectionTitle("🧪 Experiments"))
    }

    @Test
    fun `keeps plain titles whole`() {
        assertEquals(null to "Bulk Edit", splitSectionTitle("Bulk Edit"))
        assertEquals(null to "Settings", splitSectionTitle("Settings"))
    }
}
