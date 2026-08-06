package com.inscopelabs.abx.xtools.dispatcher

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.server.contractdispatcher.DispatcherContractConstants
import com.inscopelabs.abx.server.contractdispatcher.DispatcherRequest
import com.inscopelabs.abx.server.contractdispatcher.IDispatcherExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DispatcherExecutorServiceTest {

    private lateinit var context: Context
    private lateinit var service: DispatcherExecutorService
    private lateinit var binder: IDispatcherExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val controller = Robolectric.buildService(DispatcherExecutorService::class.java)
        service = controller.create().get()

        val intent = Intent(DispatcherContractConstants.SERVICE_ACTION)
        val iBinder = service.onBind(intent)
        assertNotNull("onBind must return non-null binder for SERVICE_ACTION", iBinder)
        binder = IDispatcherExecutor.Stub.asInterface(iBinder)
    }

    @Test
    fun `onBind returns null for non-dispatcher intent action`() {
        val wrongIntent = Intent("com.example.WRONG_ACTION")
        assertNull("onBind must return null for unrecognized intent action", service.onBind(wrongIntent))
    }

    @Test
    fun `execute returns error on protocol version mismatch`() {
        val request = DispatcherRequest(
            "Hello",
            "com.example.driver",
            emptyMap(),
            "test-session",
            999 // Wrong version
        )

        val response = binder.execute(request)
        assertFalse("Response success must be false on protocol mismatch", response.success)
        assertEquals(
            DispatcherContractConstants.ERROR_CODE_PROTOCOL_VERSION_MISMATCH,
            response.errorCode
        )
        assertEquals("Protocol version mismatch", response.errorMessage)
    }

    @Test
    fun `execute fails closed when driver profile is missing or disabled`() {
        val request = DispatcherRequest(
            "Hello",
            "com.example.unregistered.driver",
            emptyMap(),
            "test-session",
            DispatcherContractConstants.PROTOCOL_VERSION
        )

        val response = binder.execute(request)
        assertFalse("Response success must be false for unregistered driver", response.success)
        assertTrue("Error message should mention access denied", response.errorMessage?.contains("Access denied") == true)
    }

    @Test
    fun `execute returns error response when enabled driver session fails provider call`() = runTest {
        val repo = ChatDependencies.driverProfileRepository(context)
        val driverId = "com.example.enabled.testdriver"
        repo.saveProfile(DriverProfile(driverId = driverId, enabled = true))

        val request = DispatcherRequest(
            "Hello",
            driverId,
            emptyMap(),
            "test-session",
            DispatcherContractConstants.PROTOCOL_VERSION
        )

        val response = binder.execute(request)
        assertNotNull("Response must not be null", response)
        assertEquals(DispatcherContractConstants.PROTOCOL_VERSION, response.protocolVersion)
    }
}
