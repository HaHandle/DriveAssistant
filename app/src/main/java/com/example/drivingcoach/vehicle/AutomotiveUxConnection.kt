package com.example.drivingcoach.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Created only on AAOS. Unknown/disconnected states always restrict the presentation. */
class AutomotiveUxConnection(
    private val context: Context,
    private val onState: (VehicleUxState) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var generation = 0
    private var started = false

    fun start() {
        if (started) return
        started = true
        val session = ++generation
        onState(VehicleUxState.UNAVAILABLE)
        try {
            car = Car.createCar(
                context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
            ) { connectedCar, ready ->
                if (!started || session != generation) return@createCar
                if (!ready) {
                    manager = null
                    onState(VehicleUxState.UNAVAILABLE)
                    return@createCar
                }
                try {
                    val restrictionsManager = connectedCar.getCarManager(
                        Car.CAR_UX_RESTRICTION_SERVICE,
                    ) as? CarUxRestrictionsManager
                    if (restrictionsManager == null) {
                        onState(VehicleUxState.UNAVAILABLE)
                        return@createCar
                    }
                    manager = restrictionsManager
                    restrictionsManager.registerListener { restrictions ->
                        if (started && session == generation) publish(restrictions)
                    }
                    publish(restrictionsManager.currentCarUxRestrictions)
                } catch (exception: RuntimeException) {
                    Log.w("CoachUx", "Unable to read vehicle UX restrictions", exception)
                    onState(VehicleUxState.UNAVAILABLE)
                }
            }
        } catch (exception: RuntimeException) {
            Log.w("CoachUx", "Unable to connect to car service", exception)
            onState(VehicleUxState.UNAVAILABLE)
        }
    }

    private fun publish(restrictions: CarUxRestrictions?) {
        onState(
            when {
                restrictions == null -> VehicleUxState.UNAVAILABLE
                restrictions.isRequiresDistractionOptimization || restrictions.activeRestrictions != 0 ->
                    VehicleUxState.RESTRICTED
                else -> VehicleUxState.AVAILABLE
            },
        )
    }

    fun stop() {
        started = false
        generation++
        try {
            manager?.unregisterListener()
        } catch (exception: RuntimeException) {
            Log.w("CoachUx", "Unable to unregister UX listener", exception)
        } finally {
            manager = null
            try {
                car?.disconnect()
            } catch (exception: RuntimeException) {
                Log.w("CoachUx", "Unable to disconnect car service", exception)
            } finally {
                car = null
                onState(VehicleUxState.UNAVAILABLE)
            }
        }
    }
}
