package com.vyora.music.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterVersionTest {

    @Test
    fun `isRemoteNewer compares multi-part semantic versions accurately`() {
        assertTrue(AppUpdater.isRemoteNewer(installed = "1.1.9", remote = "1.1.10"))
        assertTrue(AppUpdater.isRemoteNewer(installed = "1.0.19", remote = "1.1.0"))
        assertTrue(AppUpdater.isRemoteNewer(installed = "1.1.99", remote = "v1.2.0"))

        assertFalse(AppUpdater.isRemoteNewer(installed = "1.1.5", remote = "1.1.5"))
        assertFalse(AppUpdater.isRemoteNewer(installed = "1.1.0", remote = "1.0.19"))
    }
}
