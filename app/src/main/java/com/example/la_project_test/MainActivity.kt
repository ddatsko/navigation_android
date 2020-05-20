package com.example.la_project_test

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
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

    private var chartCounter = -1
    private var valuesX = ArrayList<Entry>()
    private var valuesY = ArrayList<Entry>()
    private var valuesZ = ArrayList<Entry>()

    private var startTime = 0F

    private lateinit var errorVector: Vector




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
                if (counter == -1 && linearAccelerationWrapper.size() < 2500) {
                    linearAccelerationWrapper.registerValue(Vector(event.values))
                } else if (counter != -1)
                    linearAccelerationWrapper.registerValue(Vector(event.values))
                if (linearAccelerationWrapper.size() == fixesBetweenChange && counter != -1) {
                    val time = linearAccelerationWrapper.timeElapsed()
                    val avg = linearAccelerationWrapper.getAvgValue()
                    linearAccelerationWrapper.clearValues()
                    updateMovement(
                        avg,
                        time
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (counter != -1)
                    gyroscopeWrapper.registerValue(Vector(event.values))
                if (gyroscopeWrapper.size() == 10 && counter != -1) {
                    val time = gyroscopeWrapper.timeElapsed()
                    val avg = gyroscopeWrapper.getAvgValue()
                    gyroscopeWrapper.clearValues()
                    updateRotation(avg, time)
                }
            }


        }
        if (counter == -1 && linearAccelerationWrapper.size() == 2500 && accelerometerWrapper.size() == fixesBetweenChange && magnetometerWrapper.size() == fixesBetweenChange) {
            val accelerometerValue = accelerometerWrapper.getAvgValue()
            val magnetometerValue = magnetometerWrapper.getAvgValue()

            calculateInitial(accelerometerValue, magnetometerValue)
            updateViewValues()
            errorVector = linearAccelerationWrapper.getAvgValue()
            linearAccelerationWrapper.clearValues()
            gyroscopeWrapper.clearValues()
            counter = 0
            startTime = System.nanoTime().toFloat()
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
        var newVector = accelerometerVector - errorVector
        curSpeed += pInv * (newVector * (time / 1000000000.0))
        val moved = curSpeed * (time / 1000000000.0)
        distanceMoved += moved
        showCharts()
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




    private fun showCharts() {
        if (valuesX.size > 100) return
        valuesX.add(Entry(System.nanoTime().toFloat() - startTime, distanceMoved[0].toFloat()))
        valuesY.add(Entry(System.nanoTime().toFloat() - startTime, distanceMoved[1].toFloat()))
        valuesZ.add(Entry(System.nanoTime().toFloat() - startTime, distanceMoved[2].toFloat()))

        if (valuesX.size <= 100) return



        val vl = LineDataSet(valuesX, "East")
        vl.setDrawValues(false)
        vl.lineWidth = 2f
        vl.setCircleColor(getColor(R.color.blue))
        vl.color = getColor(R.color.blue)

        val vl2 = LineDataSet(valuesY, "North")
        vl2.setDrawValues(false)
        vl2.lineWidth = 2f
        vl2.setCircleColor(getColor(R.color.green))
        vl2.color = getColor(R.color.green)

        val vl3 = LineDataSet(valuesZ, "UP")
        vl3.setDrawValues(false)
        vl3.lineWidth = 2f
        vl3.setCircleColor(getColor(R.color.red))
        vl3.color = getColor(R.color.red)



        lineChart.xAxis.labelRotationAngle = 0f
        var dataSets = ArrayList<ILineDataSet>()
        dataSets.add(vl)
        dataSets.add(vl2)
        dataSets.add(vl3)
        lineChart.data = LineData(dataSets)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.axisMinimum = 0F

    }

}
