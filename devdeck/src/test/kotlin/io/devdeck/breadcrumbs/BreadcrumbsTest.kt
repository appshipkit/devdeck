package io.devdeck.breadcrumbs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BreadcrumbsTest {

    @Before
    fun setUp() {
        Breadcrumbs.clear()
        Breadcrumbs.clock = { 0L }
    }

    @After
    fun tearDown() {
        Breadcrumbs.clear()
        Breadcrumbs.maxEntries = 300
        Breadcrumbs.clock = System::currentTimeMillis
    }

    @Test
    fun `records steps in order with their type`() {
        Breadcrumbs.screen("CheckoutActivity resumed")
        Breadcrumbs.tap("Place order")
        Breadcrumbs.add("Payment sheet shown")

        assertEquals(
            listOf(BreadcrumbType.Screen, BreadcrumbType.Tap, BreadcrumbType.Custom),
            Breadcrumbs.entries.value.map { it.type },
        )
        assertEquals("Place order", Breadcrumbs.recent(2).first().message)
    }

    @Test
    fun `keeps only the newest entries`() {
        Breadcrumbs.maxEntries = 3
        repeat(5) { Breadcrumbs.add("step $it") }

        assertEquals(listOf("step 2", "step 3", "step 4"), Breadcrumbs.entries.value.map { it.message })
    }

    @Test
    fun `formats steps as aligned text lines`() {
        Breadcrumbs.tap("Place order")

        val text = Breadcrumbs.format(Breadcrumbs.recent(1))

        assertEquals(true, text.endsWith("Tap      Place order"))
    }
}
