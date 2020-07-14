package com.michael.xx.audiorecoder;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvRecord;
    private TextView tvPlay;

    private AudioRecorder audioRecorder;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_player).setOnClickListener(this);
        findViewById(R.id.btn_recorder).setOnClickListener(this);
        findViewById(R.id.btn_recorder_paused).setOnClickListener(this);
        findViewById(R.id.btn_reinit).setOnClickListener(this);

        tvRecord = (TextView) findViewById(R.id.tv_recorder_timer);
        tvPlay = (TextView) findViewById(R.id.tv_player_timer);
        initRecorder();
        initPlayer();
    }

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
    }

    private void initRecorder() {
        audioRecorder = new AudioRecorder(new AudioRecorder.CallBack() {
            @Override
            public void recordProgress(final int progress) {
                tvRecord.post(new Runnable() {
                    @Override
                    public void run() {
                        tvRecord.setText("" + progress);
                    }
                });
            }

            @Override
            public void volumn(int volumn) {

            }
        }, new WavEncoder());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_player:
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(audioRecorder.getVoiceFilePath());
                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_recorder:
                audioRecorder.startRecord();
                break;
            case R.id.btn_recorder_paused:
                audioRecorder.stop();
                break;
            case R.id.btn_reinit:
                initRecorder();
                break;
        }
    }
}
