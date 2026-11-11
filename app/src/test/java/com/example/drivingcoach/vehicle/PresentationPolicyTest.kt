package com.example.drivingcoach.vehicle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationPolicyTest {
    @Test
    fun restrictionsAndMissingVehicleStateAlwaysHideDetailsAndStopMotion() {
        for (vehicle in listOf(VehicleUxState.RESTRICTED, VehicleUxState.UNAVAILABLE)) {
            for (previewRestricted in listOf(false, true)) {
                val policy = PresentationPolicy.resolve(vehicle, previewRestricted, true, true)
                assertFalse(policy.allowDetails)
                assertFalse(policy.allowMotion)
                assertFalse(policy.showSampleData)
            }
        }
    }

    @Test
    fun previewControlsCannotOverrideActualVehicleRestrictions() {
        val policy = PresentationPolicy.resolve(VehicleUxState.RESTRICTED, false, true, true)
        assertFalse(policy.allowDetails)
        assertFalse(policy.allowMotion)
    }

    @Test
    fun sampleDataIsExclusiveToTheNonAutomotivePreview() {
        VehicleUxState.entries.forEach { vehicle ->
            val policy = PresentationPolicy.resolve(vehicle, false, true, true)
            assertTrue(policy.showSampleData == (vehicle == VehicleUxState.PREVIEW))
        }
    }

    @Test
    fun userMotionPreferenceAndBackgroundBothStopMotionWithoutHidingAvailableDetails() {
        for (vehicle in listOf(VehicleUxState.PREVIEW, VehicleUxState.AVAILABLE)) {
            val reduced = PresentationPolicy.resolve(vehicle, false, false, true)
            assertTrue(reduced.allowDetails)
            assertFalse(reduced.allowMotion)
            val background = PresentationPolicy.resolve(vehicle, false, true, false)
            assertTrue(background.allowDetails)
            assertFalse(background.allowMotion)
        }
    }

    @Test
    fun previewCanEnterAndLeaveTheSameRestrictedPresentation() {
        assertTrue(PresentationPolicy.resolve(VehicleUxState.PREVIEW, false, true, true).allowMotion)
        val restricted = PresentationPolicy.resolve(VehicleUxState.PREVIEW, true, true, true)
        assertFalse(restricted.allowDetails)
        assertFalse(restricted.allowMotion)
        assertTrue(restricted.showSampleData)
        assertTrue(PresentationPolicy.resolve(VehicleUxState.PREVIEW, false, true, true).allowDetails)
    }
}
