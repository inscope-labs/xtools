package com.inscopelabs.abx.xtools.dispatcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DriverIsolationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `chatManagerForDriver returns null for unregistered driver`() = runTest {
        val manager = ChatDependencies.chatManagerForDriver(context, "com.example.unregistered.driver")
        assertNull("Unregistered driver must return null (fail-closed)", manager)
    }

    @Test
    fun `chatManagerForDriver returns null for registered but disabled driver`() = runTest {
        val repo = ChatDependencies.driverProfileRepository(context)
        val driverId = "com.example.disabled.driver"
        repo.saveProfile(DriverProfile(driverId = driverId, enabled = false))

        val manager = ChatDependencies.chatManagerForDriver(context, driverId)
        assertNull("Disabled driver profile must return null (fail-closed)", manager)
    }

    @Test
    fun `chatManagerForDriver returns ChatManager for enabled driver`() = runTest {
        val repo = ChatDependencies.driverProfileRepository(context)
        val driverId = "com.example.enabled.driver"
        repo.saveProfile(DriverProfile(driverId = driverId, enabled = true))

        val manager = ChatDependencies.chatManagerForDriver(context, driverId)
        assertNotNull("Enabled driver profile must return an isolated ChatManager instance", manager)
    }
}
