package n.nefyra.exo

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import n.nefyra.exo.databinding.ActivityMainBinding
import n.nefyra.exo.databinding.CustomPlayerControlsBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var controlsBinding: CustomPlayerControlsBinding
    private var player: ExoPlayer? = null
    private lateinit var seekBar: SeekBar
    private lateinit var exoPosition: TextView
    private val handler = Handler()
    private var isUserSeeking = false
    private var shouldSeekToInitialTime = true
    private var initialSeekTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        controlsBinding = CustomPlayerControlsBinding.bind(
            mainBinding.playerView.findViewById(R.id.custom_player_controls_root)
        seekBar = controlsBinding.seekBar
        exoPosition = controlsBinding.exoPosition

        player = ExoPlayer.Builder(this).build()
        mainBinding.playerView.player = player

        val config = JsonUtil.readUrlFromPrivateStorage(this)
        if (config != null && config.initialValue != null) {
            val targetEp = config.initialValue.ep
            val videoUrl = config.episodes[targetEp]

            if (videoUrl != null) {
                val videoUri = Uri.parse(videoUrl)
                playVideo(videoUri)
            } else {
                Toast.makeText(this, "指定剧集不存在", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "配置文件读取失败", Toast.LENGTH_SHORT).show()
        }

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    seekBar.max = player?.duration?.toInt() ?: 0
                    startProgressUpdate()

                    if (shouldSeekToInitialTime && config?.initialValue != null) {
                        initialSeekTime = config.initialValue.time
                        if (initialSeekTime > 0 && initialSeekTime < (player?.duration ?: 0)) {
                            player?.seekTo(initialSeekTime)
                            shouldSeekToInitialTime = false
                        }
                    }
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (!isUserSeeking) {
                    seekBar.secondaryProgress = player?.bufferedPosition?.toInt() ?: 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error: ${error.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "播放失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
                handler.removeCallbacks(updateProgress)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    exoPosition.text = formatTime(progress.toLong())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                player?.seekTo(seekBar.progress.toLong())
                startProgressUpdate()
            }
        })
    }

    private fun playVideo(videoUri: Uri) {
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))
        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.play()
    }

    private fun startProgressUpdate() {
        handler.postDelayed(updateProgress, 1000)
    }

    private val updateProgress = object : Runnable {
        override fun run() {
            if (player != null && player!!.isPlaying && !isUserSeeking) {
                val position = player!!.currentPosition
                val buffered = player!!.bufferedPosition
                seekBar.progress = position.toInt()
                seekBar.secondaryProgress = buffered.toInt()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateProgress)
        player?.release()
        player = null
        super.onDestroy()
    }
}