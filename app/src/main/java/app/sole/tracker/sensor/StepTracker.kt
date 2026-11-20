package app.sole.tracker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import app.sole.tracker.data.PrefsManager
import app.sole.tracker.data.StepDatabaseHelper
import java.time.LocalDate

object StepTracker {

    fun syncOnce(context: Context, onUpdated: (() -> Unit)? = null) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val prefs = PrefsManager(context)
        val db = StepDatabaseHelper(context)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sensorManager.unregisterListener(this)
                processReading(event.values[0], prefs, db)
                onUpdated?.invoke()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun processReading(totalSinceBoot: Float, prefs: PrefsManager, db: StepDatabaseHelper) {
        val todayIso = LocalDate.now().toString()
        val lastTotal = prefs.lastSensorTotal
        val lastDate = prefs.lastSensorDate

        if (lastTotal < 0f || totalSinceBoot < lastTotal) {
            // First reading ever or device rebooted
            prefs.lastSensorTotal = totalSinceBoot
            prefs.lastSensorDate = todayIso
            db.getOrCreateDay(todayIso, prefs.dailyStepGoal)
            return
        }

        val delta = (totalSinceBoot - lastTotal).toInt()
        if (delta > 0) {
            val targetDate = if (lastDate.isNotEmpty()) lastDate else todayIso
            db.addSteps(targetDate, delta, prefs.dailyStepGoal)
        }
        prefs.lastSensorTotal = totalSinceBoot
        prefs.lastSensorDate = todayIso
        db.getOrCreateDay(todayIso, prefs.dailyStepGoal)
    }
}
