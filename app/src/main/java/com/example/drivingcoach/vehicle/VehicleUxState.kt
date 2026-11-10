package com.example.drivingcoach.vehicle

/** PREVIEW is available only on devices without the automotive system feature. */
enum class VehicleUxState { PREVIEW, AVAILABLE, RESTRICTED, UNAVAILABLE }

data class PresentationPolicy(
    val showSampleData: Boolean,
    val allowDetails: Boolean,
    val allowMotion: Boolean,
) {
    companion object {
        fun resolve(
            vehicle: VehicleUxState,
            previewRestricted: Boolean,
            motionEnabled: Boolean,
            foreground: Boolean,
        ): PresentationPolicy {
            val preview = vehicle == VehicleUxState.PREVIEW
            val available = if (preview) !previewRestricted else vehicle == VehicleUxState.AVAILABLE
            return PresentationPolicy(preview, available, available && motionEnabled && foreground)
        }
    }
}