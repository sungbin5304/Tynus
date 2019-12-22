package com.sungbin.fake.nusty.tynus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.sungbin.fake.nusty.tynus.utils.NotificationManager
import com.sungbin.fake.nusty.tynus.utils.Utils
import kotlin.math.abs

@Suppress("DEPRECATION")
class StepCountingService : Service(), SensorEventListener {
    private var lastTime: Long = 0
    private var speed = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var x = 0f
    private var y = 0f
    private var z = 0f
    private var sensorManager: SensorManager? = null
    private var accelerormeterSensor: Sensor? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerormeterSensor = sensorManager!!
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        if (accelerormeterSensor != null) sensorManager!!.registerListener(
            this, accelerormeterSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sensorManager != null) sensorManager!!.unregisterListener(this)
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int
    ) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            val gabOfTime = currentTime - lastTime
            if (gabOfTime > 100) {
                lastTime = currentTime
                x = event.values[SensorManager.DATA_X]
                y = event.values[SensorManager.DATA_Y]
                z = event.values[SensorManager.DATA_Z]
                speed = abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 10000
                if (speed > SHAKE_THRESHOLD) {
                    val preStep = Utils.readData(applicationContext,
                        "step", "0")!!.toInt()
                    val goalStep = Utils.readData(applicationContext,
                        "gaolStep", "10000")!!
                    Utils.saveData(applicationContext,
                        "step", "${preStep+1}")
                    NotificationManager.showNormalNotification(
                        applicationContext,
                        500,
                        "${preStep+1} 걸음!",
                        "목표 걸음수 : $goalStep 걸음",
                        false
                    )
                }
                lastX = event.values[DATA_X]
                lastY = event.values[DATA_Y]
                lastZ = event.values[DATA_Z]
            }
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD = 800
        private const val DATA_X = SensorManager.DATA_X
        private const val DATA_Y = SensorManager.DATA_Y
        private const val DATA_Z = SensorManager.DATA_Z
    }
}