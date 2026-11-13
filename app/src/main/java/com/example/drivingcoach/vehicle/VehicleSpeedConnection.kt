package com.example.drivingcoach.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.drivingcoach.analysis.SpeedSample

/** Reports raw vehicle timestamps and m/s. A missing or invalid signal is reported as null. */
internal class VehicleSpeedConnection(
    private val context: Context,
    private val onSample: (SpeedSample?) -> Unit,
) {
    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var callback: CarPropertyManager.CarPropertyEventCallback? = null
    private var active = false
    private var generation = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        if (active) return
        active = true
        val session = ++generation
        onSample(null)
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!active || generation != session) return@createCar
                if (!ready) {
                    unsubscribe()
                    onSample(null)
                } else {
                    try {
                        properties = connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                        subscribe()
                    } catch (error: RuntimeException) {
                        Log.w(TAG, "Vehicle speed service unavailable", error)
                        onSample(null)
                    }
                }
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Vehicle speed connection failed", error)
            onSample(null)
        }
    }

    @Suppress("DEPRECATION")
    fun subscribe() {
        val manager = properties ?: return
        if (!active || callback != null) return
        if (context.checkSelfPermission(SPEED_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            onSample(null)
            return
        }
        val listener = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (!active || properties !== manager ||
                    value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
                val speed = if (value.status == CarPropertyValue.STATUS_AVAILABLE) value.value as? Float else null
                publish(if (speed != null && speed.isFinite() && value.timestamp > 0L) {
                    SpeedSample(value.timestamp, speed)
                } else null)
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (active && properties === manager &&
                    propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED) publish(null)
            }
        }
        try {
            if (manager.registerCallback(
                    listener, VehiclePropertyIds.PERF_VEHICLE_SPEED,
                    CarPropertyManager.SENSOR_RATE_NORMAL,
                )) callback = listener else onSample(null)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Vehicle speed subscription failed", error)
            onSample(null)
        }
    }

    fun stop() {
        active = false
        generation++
        unsubscribe()
        try {
            car?.disconnect()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Vehicle speed disconnect failed", error)
        } finally {
            car = null
            onSample(null)
        }
    }

    private fun unsubscribe() {
        try {
            callback?.let { properties?.unregisterCallback(it) }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Vehicle speed unsubscribe failed", error)
        } finally {
            callback = null
            properties = null
        }
    }

    private fun publish(sample: SpeedSample?) {
        val session = generation
        mainHandler.post {
            if (active && generation == session) onSample(sample)
        }
    }

    private companion object {
        const val TAG = "CoachSpeed"
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
    }
}
