package io.devdeck.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppPermissionTest {

    @Test
    fun `android permissions get a readable name and no owner`() {
        val permission = AppPermission("android.permission.POST_NOTIFICATIONS", granted = false)

        assertEquals("Post notifications", permission.displayName)
        assertNull(permission.owner)
    }

    @Test
    fun `app-defined permissions show their owning package`() {
        val permission = AppPermission("io.devdeck.sample.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION", granted = true)

        assertEquals("Dynamic receiver not exported permission", permission.displayName)
        assertEquals("io.devdeck.sample", permission.owner)
    }
}
