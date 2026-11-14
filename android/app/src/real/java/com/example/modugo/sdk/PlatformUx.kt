package com.example.modugo.sdk

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.drivingstate.CarUxRestrictions
import android.content.Context
import android.os.Handler
import android.os.Looper

/** 플랫폼의 조작 허용 상태를 사용하며 차량 속도나 주차 기어를 판정하지 않는다. */
class PlatformUx(private val context: Context, private val changed: (Boolean) -> Unit) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var active = false
    private fun update(value: CarUxRestrictions?) {
        if (active) changed(value != null && !value.isRequiresDistractionOptimization &&
            value.activeRestrictions == CarUxRestrictions.UX_RESTRICTIONS_BASELINE)
    }
    fun start() {
        active = true
        car = Car.createCar(context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
            if (!active) return@createCar
            if (!ready) { manager = null; changed(false); return@createCar }
            try {
                manager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                manager?.registerListener { update(it) }
                update(manager?.currentCarUxRestrictions)
            } catch (_: Exception) { changed(false) }
        }
    }
    fun stop() {
        active = false
        try { manager?.unregisterListener() } catch (_: Exception) { }
        try { car?.disconnect() } catch (_: Exception) { }
        manager = null; car = null
    }
}
