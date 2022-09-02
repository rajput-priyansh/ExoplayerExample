package com.vibs.exoplayerexample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vibs.exoplayerexample.databinding.ActivityMainBinding
import com.vibs.exoplayerexample.utils.toast
import com.vibs.previewseekbar.timebar.PreviewTimeBar
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() , SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var previewTimeBar: PreviewTimeBar
    private lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var sensorManager: SensorManager
    private var sensors: Sensor? = null

    private var accelCurrent: Float = 0f
    private var accelLast: Float = 0f
    private var accel: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initData()

        initUi()
    }

    override fun onStart() {
        super.onStart()
        exoPlayerManager.onStart()
    }

    override fun onResume() {
        super.onResume()
        //suitable for screen orientation changes
        sensorManager.registerListener(this, sensors, SensorManager.SENSOR_DELAY_NORMAL)
        exoPlayerManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        exoPlayerManager.onPause()
    }

    override fun onStop() {
        super.onStop()
        exoPlayerManager.onStop()

    }

    /**
     * Use to init App Data
     */
    private fun initData() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // constant describing a light sensor type.
        sensors = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    /**
     * Init All UI components
     */
    private fun initUi() {
        previewTimeBar = binding.videoView.findViewById(R.id.exo_progress)

        exoPlayerManager = ExoPlayerManager(this, binding.videoView, previewTimeBar,
            binding.videoView.findViewById(R.id.imageView))
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if ((event?.values?.size ?: 0) < 3)
            return

        val x: Float = event?.values?.get(0)?:0f
        val y: Float = event?.values?.get(1)?:0f
        val z: Float = event?.values?.get(2)?:0f
        accelLast = accelCurrent
        accelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta: Float = accelCurrent - accelLast
        accel = accel * 0.9f + delta

        if (accel > 25) {
            exoPlayerManager.pausePlayer()
            toast(getString(R.string.mse_your_phone_shaken))
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}