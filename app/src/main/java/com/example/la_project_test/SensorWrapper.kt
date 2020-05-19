package com.example.la_project_test

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorWrapper(
    var manager: SensorManager,
    var sensor: Sensor,
    var listener: SensorEventListener
) {

    private var values = ArrayList<Vector>()
    private var fixTime: Long = 0
    var maxCount = 1000

    init {
        fixTime = System.nanoTime()
    }

    fun registerListener() {
        values.clear()
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        fixTime = System.nanoTime()
    }

    fun registerValue(v: Vector) {
        values.add(v)

    }

    fun timeElapsed(): Long {
        return System.nanoTime() - fixTime
    }

    fun clearListener() {
        manager.unregisterListener(listener, sensor)
    }

    fun clearValues() {
        values.clear()
        fixTime = System.nanoTime()
    }


    fun size() = values.size

    fun getAvgValue() = values.reduce(Vector::plus) * (1.0 / values.size)
}