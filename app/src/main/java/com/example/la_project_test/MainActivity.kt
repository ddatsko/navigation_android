package com.example.la_project_test

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.cos
import java.lang.Math.sin
import kotlin.random.Random


class MainActivity : AppCompatActivity(), SensorEventListener, View.OnClickListener {
    // Own calculations
    private var x = Vector()
    private var y = Vector()
    private var z = Vector()

    private lateinit var accelerometerWrapper: SensorWrapper
    private lateinit var magnetometerWrapper: SensorWrapper
    private lateinit var linearAccelerationWrapper: SensorWrapper
    private lateinit var gyroscopeWrapper: SensorWrapper

    private var initialization = false
    private var initialized = false

    private lateinit var p: Matrix // From global to smartphone
    private lateinit var pInv: Matrix // From smartphone to global

    private val fixesBetweenChange = 10
    private var counter = -1
    private var checked = false // to make button be pressed only one time (problems with listeners)

    private lateinit var curSpeed: Vector
    private lateinit var distanceMoved: Vector

    // Sensors
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var linearAcceleration: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var gyroscope: Sensor




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelerometerWrapper = SensorWrapper(sensorManager, accelerometer, this)
        magnetometerWrapper = SensorWrapper(sensorManager, magnetometer, this)
        linearAccelerationWrapper = SensorWrapper(sensorManager, linearAcceleration, this)
        gyroscopeWrapper = SensorWrapper(sensorManager, gyroscope, this)

        curSpeed = Vector(0.0, 0.0, 0.0)
        distanceMoved = Vector(0.0, 0.0, 0.0)


        button.setOnClickListener(this)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Unit
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.button -> {
                if (checked) return
                checked = true
                initialization = true
                initialized = false
                accelerometerWrapper.registerListener()
                magnetometerWrapper.registerListener()
                linearAccelerationWrapper.registerListener()
                gyroscopeWrapper.registerListener()
            }
        }
    }

    private fun calculateInitial(accelerometerValues: Vector, magnetometerValues: Vector) {
        accelerometerValues.normalize()
        magnetometerValues.normalize()

        z = accelerometerValues
        z.normalize()
        y = magnetometerValues - z * (z.innerProduct(magnetometerValues))
        y.normalize()
        x = y.crossProduct(z)
        x.normalize()


        p = Matrix(x, y, z)
        pInv = p.inverse()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (accelerometerWrapper.size() < fixesBetweenChange) {
                    accelerometerWrapper.registerValue(Vector(event.values))
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (magnetometerWrapper.size() < fixesBetweenChange) {
                    magnetometerWrapper.registerValue(Vector(event.values))
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                linearAccelerationWrapper.registerValue(Vector(event.values))
                if (linearAccelerationWrapper.size() == fixesBetweenChange && counter != -1) {
                    updateMovement(
                        linearAccelerationWrapper.getAvgValue(),
                        linearAccelerationWrapper.timeElapsed()
                    )

                    linearAccelerationWrapper.clearValues()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeWrapper.registerValue(Vector(event.values))
                if (gyroscopeWrapper.size() == 10 && counter != -1) {
                    print("Hello")
                    updateRotation(gyroscopeWrapper.getAvgValue(), gyroscopeWrapper.timeElapsed())
                    gyroscopeWrapper.clearValues()
                }
            }


        }
        if (counter == -1 && accelerometerWrapper.size() == fixesBetweenChange && magnetometerWrapper.size() == fixesBetweenChange) {
            val accelerometerValue = accelerometerWrapper.getAvgValue()
            val magnetometerValue = magnetometerWrapper.getAvgValue()

            calculateInitial(accelerometerValue, magnetometerValue)
            updateViewValues()
            linearAccelerationWrapper.clearValues()
            gyroscopeWrapper.clearValues()
            counter = 0
        }

        if (counter >= 0)  {
            updateViewValues()
            counter = 0
        }

    }

    private fun updateViewValues() {
        val builder = StringBuilder()
        builder.append(pInv * Vector(0.0, 1.0, 0.0))
        rotationVectorData2.text = builder.toString()
        tmpText.text = distanceMoved.toString()
    }


    private fun updateMovement(accelerometerVector: Vector, time: Long) {
        curSpeed += accelerometerVector * (time / 1000000000.0)
        var moved = curSpeed * (time / 1000000000.0)
        distanceMoved += pInv * moved

    }

    private fun updateRotation(gyroscopeVector: Vector, time: Long) {

        // Current X, Y, Z of phone in phone coordinate system
        var currentCoordinates = Matrix.identity()

        val thetaX = gyroscopeVector[0] * time / 1000000000.0  // in radians
        val thetaY = gyroscopeVector[1] * time / 1000000000.0 // in radians
        val thetaZ = gyroscopeVector[2] * time / 1000000000.0  // in radians

        // Consider rotation along X

        // NOTE: vectors represent columns (a little confusing notation here)
        val rotationOverXMatrix = Matrix(
            Vector(1.0, 0.0, 0.0),
            Vector(0.0, kotlin.math.cos(thetaX), kotlin.math.sin(thetaX)),
            Vector(0.0, -kotlin.math.sin(thetaX), kotlin.math.cos(thetaX))
        )
        val rotationOverYMatrix = Matrix(
            Vector(kotlin.math.cos(thetaY), 0.0, -kotlin.math.sin(thetaY)),
            Vector(0.0, 1.0, 0.0),
            Vector(kotlin.math.sin(thetaY), 0.0, kotlin.math.cos(thetaY))
        )
        val rotationOverZMatrix = Matrix(
            Vector(kotlin.math.cos(thetaZ), kotlin.math.sin(thetaZ), 0.0),
            Vector(-kotlin.math.sin(thetaZ), kotlin.math.cos(thetaZ), 0.0),
            Vector(0.0, 0.0, 1.0)
        )
        pInv = pInv * rotationOverXMatrix * rotationOverYMatrix * rotationOverZMatrix

    }


//
//    private fun updateChartValues() {
//        counter++
//        if (counter < 0) return
//        if (counter > 1000) {
//            showCharts(xValues[xValues.size - 1].x)
//            counter = -1000000
//        }
//        textView7.text = counter.toString()
//
//        xValues.add(Entry((System.nanoTime() - startTime).toFloat(), accelerometerValues[0]))
//        yValues.add(Entry((System.nanoTime() - startTime).toFloat(), accelerometerValues[1]))
//        zValues.add(Entry((System.nanoTime() - startTime).toFloat(), accelerometerValues[2]))
//
//
//    }

//    private fun showCharts(xMax: Float) {
//        val vl = LineDataSet(xValues, "X")
//        vl.setDrawValues(false)
//        vl.lineWidth = 2f
//        vl.setCircleColor(getColor(R.color.blue))
//        vl.color = getColor(R.color.blue)
//
//        val vl2 = LineDataSet(yValues, "Y")
//        vl2.setDrawValues(false)
//        vl2.lineWidth = 2f
//        vl2.setCircleColor(getColor(R.color.green))
//        vl2.color = getColor(R.color.green)
//
//        val vl3 = LineDataSet(zValues, "Z")
//        vl3.setDrawValues(false)
//        vl3.lineWidth = 2f
//        vl3.setCircleColor(getColor(R.color.red))
//        vl3.color = getColor(R.color.red)
//
//
//
//        lineChart.xAxis.labelRotationAngle = 0f
//        var dataSets = ArrayList<ILineDataSet>()
//        dataSets.add(vl)
//        dataSets.add(vl2)
//        dataSets.add(vl3)
//        lineChart.data = LineData(dataSets)
//        lineChart.axisRight.isEnabled = false
//        lineChart.xAxis.axisMinimum = 0F
//        lineChart.xAxis.axisMaximum = xMax + 1F
//
//    }

}
