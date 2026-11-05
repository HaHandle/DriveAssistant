package com.example.drivingcoach

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
internal class CarRestrictionsMonitor(
    private val context: Context,
    private val onRestrictionChanged: (Boolean?) -> Unit
) {
    private var active = false
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        active = true
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (active) {
                    if (ready) {
                        subscribe(connectedCar)
                    } else {
                        manager = null
                        onRestrictionChanged(null)
                    }
                }
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Car service connection failed", error)
            onRestrictionChanged(null)
        } catch (error: LinkageError) {
            Log.w(TAG, "Car library unavailable", error)
            onRestrictionChanged(null)
        }
    }

    private fun subscribe(connectedCar: Car) {
        try {
            val restrictions = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
                as? CarUxRestrictionsManager
            manager = restrictions
            restrictions?.registerListener { value ->
                if (active && manager === restrictions) {
                    onRestrictionChanged(value?.isRequiresDistractionOptimization())
                }
            }
            onRestrictionChanged(
                restrictions?.currentCarUxRestrictions?.isRequiresDistractionOptimization()
            )
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not read vehicle UX restrictions", error)
            onRestrictionChanged(null)
        }
    }

    fun stop() {
        active = false
        try {
            manager?.unregisterListener()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not unregister vehicle UX listener", error)
        } finally {
            manager = null
            car?.disconnect()
            car = null
        }
    }

    private companion object {
        const val TAG = "CarRestrictionsMonitor"
    }
}
