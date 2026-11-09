package com.example.drivingcoach

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class PleosConnectionTest {
    @Test
    fun vehicleSpeedAndPlatformRestrictionChangesReachTheApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, "android.car.permission.CAR_SPEED"
        )
        val states = LinkedBlockingQueue<Boolean>()
        val speeds = LinkedBlockingQueue<SpeedSample>()
        lateinit var monitor: CarRestrictionsMonitor
        instrumentation.runOnMainSync {
            monitor = CarRestrictionsMonitor(
                context,
                onRestrictionChanged = { state -> state?.let { states.offer(it) } },
                onSpeedChanged = { sample -> sample?.let { speeds.offer(it) } }
            )
            monitor.start()
        }
        fun setRestriction(enabled: Boolean) {
            val state = if (enabled) "on" else "off"
            val output = instrumentation.uiAutomation.executeShellCommand(
                "cmd hmg_car_service --service drivingstate driving_regulation $state"
            )
            ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        }
        fun awaitRestriction(expected: Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 45_000
            while (SystemClock.elapsedRealtime() < deadline) {
                if (states.poll(1, TimeUnit.SECONDS) == expected) return
            }
            throw AssertionError("No platform restriction callback for state $expected")
        }
        try {
            val speed = speeds.poll(90, TimeUnit.SECONDS)
            assertNotNull("No usable PERF_VEHICLE_SPEED sample received", speed)
            assertTrue(speed!!.metersPerSecond.isFinite())
            assertTrue(speed.timestampNanos > 0)
            setRestriction(false)
            awaitRestriction(false)
            states.clear()
            setRestriction(true)
            awaitRestriction(true)
            states.clear()
            setRestriction(false)
            awaitRestriction(false)
        } finally {
            try {
                setRestriction(false)
            } finally {
                instrumentation.runOnMainSync { monitor.stop() }
            }
        }
    }
}
