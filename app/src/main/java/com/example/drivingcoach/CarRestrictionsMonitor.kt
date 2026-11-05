package com.example.drivingcoach

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
internal class CarRestrictionsMonitor(
    private val context: Context,
    private val onRestrictionChanged: (Boolean?) -> Unit,
    private val onSpeedChanged: (SpeedSample?) -> Unit
) {
    private var active = false
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private var properties: CarPropertyManager? = null
    private var speedCallback: CarPropertyManager.CarPropertyEventCallback? = null

    fun start() {
        active = true
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (active) {
                    if (ready) {
                        subscribe(connectedCar)
                        try {
                            properties = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                            refreshSpeedSubscription()
                        } catch (error: RuntimeException) {
                            Log.w(TAG, "Vehicle speed service unavailable", error)
                            onSpeedChanged(null)
                        }
                    } else {
                        properties = null
                        speedCallback = null
                        onSpeedChanged(null)
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

    @Suppress("DEPRECATION")
    fun refreshSpeedSubscription() {
        val propertyManager = properties ?: return
        if (speedCallback != null) return
        if (context.checkSelfPermission(SPEED_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            onSpeedChanged(null)
            return
        }
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (!active || properties !== propertyManager) return
                if (value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
                val speed = if (value.status == CarPropertyValue.STATUS_AVAILABLE) value.value as? Float else null
                if (value.status == CarPropertyValue.STATUS_AVAILABLE && speed != null && speed.isFinite()) {
                    onSpeedChanged(SpeedSample(value.timestamp, speed))
                } else {
                    onSpeedChanged(null)
                }
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (active && properties === propertyManager) onSpeedChanged(null)
            }
        }
        try {
            val registered = propertyManager.registerCallback(
                callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL
            )
            if (registered) speedCallback = callback else onSpeedChanged(null)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not subscribe to vehicle speed", error)
            onSpeedChanged(null)
        }
    }

    fun stop() {
        active = false
        try {
            speedCallback?.let { properties?.unregisterCallback(it) }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not unregister vehicle speed listener", error)
        } finally {
            speedCallback = null
            properties = null
            onSpeedChanged(null)
        }
        try {
            manager?.unregisterListener()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not unregister vehicle UX listener", error)
        } finally {
            manager = null
            try {
                car?.disconnect()
            } finally {
                car = null
            }
        }
    }

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
        const val TAG = "CarRestrictionsMonitor"
    }
}
