package com.vibs.exoplayerexample

import android.content.Context

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.vibs.previewseekbar.PreviewBar
import com.vibs.previewseekbar.PreviewLoader
import com.vibs.previewseekbar.timebar.PreviewTimeBar


class ExoPlayerManager(
    private val context: Context,
    private val playerView: PlayerView,
    private val previewTimeBar: PreviewTimeBar, private val imageView: ImageView
) : PreviewLoader, PreviewBar.OnScrubListener {
    private var mediaUri: Uri? = null

    private var isPlayWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onScrubStart(previewBar: PreviewBar) {
        playerView.player?.playWhenReady = false
    }

    override fun onScrubMove(previewBar: PreviewBar, progress: Int, fromUser: Boolean) {
        //Empty Body
    }

    override fun onScrubStop(previewBar: PreviewBar) {
        playerView.player?.playWhenReady = true

    }

    override fun loadPreview(currentPosition: Long, max: Long) {
        //Stop play the video
        if (playerView.player?.isPlaying == true) {
            playerView.player?.playWhenReady = false
        }
        //Set the preview
        mediaUri?.let { uri ->
            Glide.with(context).asBitmap()
                .load(uri)
                .apply(RequestOptions().frame(currentPosition * 1000))//microseconds
                .placeholder(R.drawable.video_frame_alternative)
                .into(imageView)
        }
    }

    /**
     * Manage the Player onStart life cycle
     */
    fun onStart() {
        if (Util.SDK_INT > 23) {
            createPlayer()
        }
    }

    /**
     * Manage the Player onResume life cycle
     */
    fun onResume() {
        if (Util.SDK_INT <= 23) {
            createPlayer()
        }
    }

    /**
     * Manage the Player onPause life cycle
     */
    fun onPause() {
        if (Util.SDK_INT <= 23) {
            releasePlayers()
        }
    }

    /**
     * Manage the Player onStop life cycle
     */
    fun onStop() {
        if (Util.SDK_INT > 23) {
            releasePlayers()
        }
    }

    /**
     * Manage and release the player
     * store currentPosition
     * store currentMediaItemIndex
     * store playWhenReady
     */
    private fun releasePlayers() {
        playerView.player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            isPlayWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
    }

    /**
     * Initialize the player and set the default ot restore initial values.
     */
    private fun createPlayer() {
        playerView.player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            isPlayWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }

        ExoPlayer.Builder(context).build().also { exoPlayer ->
            playerView.player = exoPlayer
            //Set up media-item
            mediaUri = Uri.parse(context.getString(R.string.url_video))
            val mediaItem =
                MediaItem.fromUri(context.getString(R.string.url_video))

            with(exoPlayer) {
                setMediaItem(mediaItem)
                playWhenReady = isPlayWhenReady
                seekTo(currentItem, playbackPosition)
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        val message =
                            when (playbackState) {
                                ExoPlayer.STATE_IDLE -> {
                                    context.getString(R.string.msg_check_network)
                                }
                                ExoPlayer.STATE_BUFFERING -> {
                                    context.getString(R.string.msg_video_idle)
                                }
                                ExoPlayer.STATE_READY -> {
                                    context.getString(R.string.msg_video_ready)
                                }
                                ExoPlayer.STATE_ENDED -> {
                                    context.getString(R.string.msg_video_end)
                                }
                                else -> {
                                    context.getString(R.string.msg_video_unknown)
                                }
                            }

                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }

                })
            }
        }
        //Sets the playback controls timeout
        playerView.controllerShowTimeoutMs = 15000
    }

    /**
     * Reset tha player
     */
    fun restartPlayer() {
        isPlayWhenReady = true
        currentItem = 0
        playbackPosition = 0L

        with(playerView.player) {
            this?.playWhenReady = isPlayWhenReady
            this?.seekTo(currentItem, playbackPosition)
        }
    }

    /**
     * pause the player
     */
    fun pausePlayer() {
        playerView.player?.pause()
    }

    //init block
    init {
        previewTimeBar.addOnScrubListener(this)
        previewTimeBar.setPreviewLoader(this)
    }
}