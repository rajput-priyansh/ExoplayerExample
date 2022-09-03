package com.vibs.exoplayerexample

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.vibs.exoplayerexample.databinding.ActivityMainBinding
import com.vibs.exoplayerexample.service.ForegroundOnlyLocationService
import com.vibs.exoplayerexample.utils.toText
import com.vibs.exoplayerexample.utils.toast
import com.vibs.previewseekbar.timebar.PreviewTimeBar
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var previewTimeBar: PreviewTimeBar
    private lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var sensorManager: SensorManager
    private var sensors: Sensor? = null

    private var accelCurrent: Float = 0f
    private var accelLast: Float = 0f
    private var accel: Float = 0f


    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            updateServiceState(isGranted)
        }

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

        //Bind the service
        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            updateServiceState(true)
        } else {
            requestPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        //suitable for screen orientation changes
        sensorManager.registerListener(this, sensors, SensorManager.SENSOR_DELAY_NORMAL)
        exoPlayerManager.onResume()

        //Register LocalBroadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        //Unregister LocalBroadcast
        LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundOnlyBroadcastReceiver)
        super.onPause()
        exoPlayerManager.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        exoPlayerManager.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        //Unbind the service on activity onDestroy.
        //updateServiceState(false)
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if ((event?.values?.size ?: 0) < 3)
            return

        val x: Float = event?.values?.get(0) ?: 0f
        val y: Float = event?.values?.get(1) ?: 0f
        val z: Float = event?.values?.get(2) ?: 0f
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
        //Empty Body
    }

    /**
     * Use to init App Data
     */
    private fun initData() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // constant describing a light sensor type.
        sensors = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        //Init BroadcastReceiver
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

    }

    /**
     * Init All UI components
     */
    private fun initUi() {
        previewTimeBar = binding.videoView.findViewById(R.id.exo_progress)

        exoPlayerManager = ExoPlayerManager(
            this, binding.videoView, previewTimeBar,
            binding.videoView.findViewById(R.id.imageView)
        )
    }

    /**
     * Check Location permission status,
     * bind or unbind service
     */
    private fun updateServiceState(enabled: Boolean) {
        if (enabled) {
            foregroundOnlyLocationService?.subscribeToLocationUpdates()
        } else {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
        }
    }

    /**
     * Check whether a permission is granted or not.
     *
     * @param permission
     * @return
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request permission and get the result on callback.
     */
    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Inner class for BroadcastReceiver
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                toast("Foreground location: ${location.toText()}")
                //reset the video and replay from the start.
                exoPlayerManager.restartPlayer()
            }
        }
    }
}