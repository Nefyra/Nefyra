package com.nefyra.exo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.nefyra.exo.databinding.ActivityMainBinding;
import com.nefyra.exo.databinding.CustomPlayerControlsBinding;

public class MainActivity extends AppCompatActivity {
  private ActivityMainBinding mainBinding;
  private CustomPlayerControlsBinding controlsBinding;
  private ExoPlayer player;
  private SeekBar seekBar;
  private TextView exoPosition;
  private Handler handler = new Handler();
  private boolean isUserSeeking = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(mainBinding.getRoot());

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    View decorView = window.getDecorView();
    int uiOptions =
        View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    decorView.setSystemUiVisibility(uiOptions);

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      try {
        WindowManager.LayoutParams params = window.getAttributes();
        params.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.setAttributes(params);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    controlsBinding =
        CustomPlayerControlsBinding.bind(
            mainBinding.playerView.findViewById(R.id.custom_player_controls_root));
    seekBar = controlsBinding.seekBar;
    exoPosition = controlsBinding.exoPosition;

    player = new ExoPlayer.Builder(this).build();
    mainBinding.playerView.setPlayer(player);

    player.addListener(
        new Player.Listener() {
          @Override
          public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
              seekBar.setMax((int) player.getDuration());
              startProgressUpdate();
            }
          }

          @Override
          public void onIsLoadingChanged(boolean isLoading) {
            if (!isUserSeeking) {
              seekBar.setSecondaryProgress((int) player.getBufferedPosition());
            }
          }

          @Override
          public void onPlayerError(PlaybackException error) {
            Log.e("ExoPlayer", "Error: " + error.getMessage());
            runOnUiThread(
                () ->
                    Toast.makeText(
                            MainActivity.this, "播放失败: " + error.getMessage(), Toast.LENGTH_SHORT)
                        .show());
          }
        });

    VideoConfig config = JsonUtil.readUrlFromPrivateStorage(this);
    if (config != null && config.getUrl() != null) {
      Uri videoUri = Uri.parse(config.getUrl());
      playVideo(videoUri);
    } else {
      Toast.makeText(this, "配置文件读取失败", Toast.LENGTH_SHORT).show();
    }

    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            isUserSeeking = true;
            handler.removeCallbacks(updateProgress);
          }

          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
              exoPosition.setText(formatTime(progress));
            }
          }

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            isUserSeeking = false;
            player.seekTo(seekBar.getProgress());
            startProgressUpdate();
          }
        });
  }

  private void playVideo(Uri videoUri) {
    DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
    ProgressiveMediaSource mediaSource =
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri));
    player.setMediaSource(mediaSource);
    player.prepare();
    player.play();
  }

  private void startProgressUpdate() {
    handler.postDelayed(updateProgress, 1000);
  }

  private Runnable updateProgress =
      new Runnable() {
        @Override
        public void run() {
          if (player != null && player.isPlaying() && !isUserSeeking) {
            long position = player.getCurrentPosition();
            long buffered = player.getBufferedPosition();
            seekBar.setProgress((int) position);
            seekBar.setSecondaryProgress((int) buffered);
          }
          handler.postDelayed(this, 1000);
        }
      };

  private String formatTime(long millis) {
    int seconds = (int) (millis / 1000) % 60;
    int minutes = (int) ((millis / 1000) / 60);
    return String.format("%02d:%02d", minutes, seconds);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacks(updateProgress);
    if (player != null) {
      player.release();
      player = null;
    }
    super.onDestroy();
  }
}
