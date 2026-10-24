package io.devdeck.breadcrumbs

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BreadcrumbDescribeTest {

    @Test
    fun `records the label of static text views`() {
        assertEquals("Place order", BreadcrumbCollector.recordableText("Place order", editable = false, inputType = InputType.TYPE_NULL))
    }

    @Test
    fun `never records text typed into editable views`() {
        assertNull(BreadcrumbCollector.recordableText("hunter2", editable = true, inputType = InputType.TYPE_CLASS_TEXT))
    }

    @Test
    fun `never records password input even when not editable`() {
        val passwords = listOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        )
        passwords.forEach { assertNull(BreadcrumbCollector.recordableText("1234", editable = false, inputType = it)) }
    }

    @Test
    fun `skips blank text`() {
        assertNull(BreadcrumbCollector.recordableText("  ", editable = false, inputType = InputType.TYPE_NULL))
    }
}
