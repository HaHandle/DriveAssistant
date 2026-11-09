package com.example.drivingcoach

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

internal const val PARK_SIGNAL_PERMISSION = "android.car.permission.CAR_POWERTRAIN"

@RequiresApi(Build.VERSION_CODES.Q)
internal class CarRestrictionsMonitor(
    private val context: Context,
    private val onRestrictionChanged: (Boolean?) -> Unit,
    private val onSpeedChanged: (SpeedSample?) -> Unit,
    private val onParkChanged: (Boolean?) -> Unit = {}
) {
    private var active = false
    private var car: Car? = null
    private var restrictions: CarUxRestrictionsManager? = null
    private var properties: CarPropertyManager? = null
    private var speedCallback: CarPropertyManager.CarPropertyEventCallback? = null
    private var parkCallback: CarPropertyManager.CarPropertyEventCallback? = null
    private var connectionGeneration = 0

    fun start() {
        if (active) return
        active = true
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (active) {
                    clearSubscriptions()
                    if (ready) subscribe(connectedCar)
                }
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Car service connection failed", error)
            clearSubscriptions()
        } catch (error: LinkageError) {
            Log.w(TAG, "Car library unavailable", error)
            clearSubscriptions()
        }
    }

    private fun subscribe(connectedCar: Car) {
        val generation = connectionGeneration
        try {
            val manager = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
                as? CarUxRestrictionsManager
            restrictions = manager
            manager?.registerListener { value ->
                if (active && generation == connectionGeneration && restrictions === manager) {
                    onRestrictionChanged(value?.isRequiresDistractionOptimization())
                }
            }
            onRestrictionChanged(manager?.currentCarUxRestrictions?.isRequiresDistractionOptimization())
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not read vehicle UX restrictions", error)
            restrictions = null
            onRestrictionChanged(null)
        }
        try {
            properties = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            refreshSpeedSubscription()
            subscribePark()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Vehicle property service unavailable", error)
            onSpeedChanged(null)
            onParkChanged(null)
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
                if (!active || speedCallback !== this || value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
                val speed = if (value.status == CarPropertyValue.STATUS_AVAILABLE) value.value as? Float else null
                onSpeedChanged(speed?.takeIf { it.isFinite() && value.timestamp > 0 }
                    ?.let { SpeedSample(value.timestamp, it) })
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (active && speedCallback === this && propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED) {
                    onSpeedChanged(null)
                }
            }
        }
        speedCallback = callback
        try {
            if (propertyManager.registerCallback(
                    callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL
                )
            ) return
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not subscribe to vehicle speed", error)
        }
        speedCallback = null
        onSpeedChanged(null)
    }

    @Suppress("DEPRECATION")
    private fun subscribePark() {
        val propertyManager = properties ?: return
        if (context.checkSelfPermission(PARK_SIGNAL_PERMISSION) != PackageManager.PERMISSION_GRANTED) return

        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            private var lastTimestamp = 0L

            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (!active || parkCallback !== this || value.propertyId != VehiclePropertyIds.GEAR_SELECTION) return
                val timestamp = value.timestamp
                if (timestamp <= 0L) {
                    onParkChanged(null)
                    return
                }
                if (timestamp <= lastTimestamp) return
                lastTimestamp = timestamp
                val gear = if (value.status == CarPropertyValue.STATUS_AVAILABLE) value.value as? Int else null
                onParkChanged(gear?.let { it == VehicleGear.GEAR_PARK })
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (active && parkCallback === this && propertyId == VehiclePropertyIds.GEAR_SELECTION) {
                    onParkChanged(null)
                }
            }
        }
        parkCallback = callback
        try {
            if (propertyManager.registerCallback(
                    callback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE
                )
            ) return
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not subscribe to gear selection", error)
        }
        parkCallback = null
        onParkChanged(null)
    }

    fun stop() {
        active = false
        clearSubscriptions()
        val connection = car
        car = null
        try {
            connection?.disconnect()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not disconnect from vehicle service", error)
        }
    }

    @Suppress("DEPRECATION")
    private fun clearSubscriptions() {
        connectionGeneration++
        val manager = properties
        val oldSpeed = speedCallback
        val oldPark = parkCallback
        val oldRestrictions = restrictions
        properties = null
        speedCallback = null
        parkCallback = null
        restrictions = null
        onRestrictionChanged(null)
        onParkChanged(null)
        onSpeedChanged(null)

        listOfNotNull(oldSpeed, oldPark).forEach { callback ->
            try {
                manager?.unregisterCallback(callback)
            } catch (error: RuntimeException) {
                Log.w(TAG, "Could not unregister a vehicle property listener", error)
            }
        }
        try {
            oldRestrictions?.unregisterListener()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not unregister vehicle UX listener", error)
        }
    }

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
        const val TAG = "CarRestrictionsMonitor"
    }
}
