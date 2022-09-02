package com.vibs.exoplayerexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vibs.exoplayerexample.databinding.ActivityMainBinding
import com.vibs.previewseekbar.timebar.PreviewTimeBar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var previewTimeBar: PreviewTimeBar
    lateinit var exoPlayerManager: ExoPlayerManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initUi()
    }

    override fun onStart() {
        super.onStart()
        exoPlayerManager.onStart()
    }

    override fun onResume() {
        super.onResume()
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
     * Init All UI components
     */
    private fun initUi() {
        previewTimeBar = binding.videoView.findViewById(R.id.exo_progress)

        exoPlayerManager = ExoPlayerManager(this, binding.videoView, previewTimeBar,
            binding.videoView.findViewById(R.id.imageView))
    }
}