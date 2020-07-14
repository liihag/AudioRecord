package com.michael.xx.audiorecoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private int audioInput = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private int audioSampleRate = 44100 ;
    private int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
//    private int audioChannel = 0xfc;
//    private int audioChannel = 0x3c;
//        private int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
    private boolean CHANNEL_INDEX_MASK=false ;
    private int audioEncode = AudioFormat.ENCODING_PCM_16BIT;
    private boolean AECenable=false;
//    private boolean AECenable=true;
    private AcousticEchoCanceler acousticEchoCanceler;

    private int bufferSizeInBytes = 0;
    private AudioRecord audioRecord;
    private Status status = Status.STATUS_NO_READY;
    protected String pcmFileName;

    private Timer timer;

    private TimerTask timerTask;
    private int currentPosition = 0;
    private CallBack callBack;
    private int lastVolumn = 0;
    private AudioEncoder encoder;

    public AudioRecorder(CallBack callBack, AudioEncoder encoder) {
        pcmFileName = AudioFileUtils.getPcmFileAbsolutePath(System.currentTimeMillis() + "");
        this.encoder = encoder;

//        encoder.init(audioSampleRate, 16, 1)·;
        File file = new File(pcmFileName);
        if (file.exists()) {
            file.delete();
        }
        status = Status.STATUS_READY;
        this.callBack = callBack;
    }


    public void setAudioInput(int audioInput) {
        this.audioInput = audioInput;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public void setAudioChannel(int audioChannel) {
        this.audioChannel = audioChannel;
    }


    public void setEncoder(AudioEncoder encoder) {
        this.encoder = encoder;
    }

    private void startTimer() {
        if (timer == null)
            timer = new Timer();
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                currentPosition++;
                if (callBack != null && status == Status.STATUS_START) {
                    callBack.recordProgress(currentPosition);
                    callBack.volumn(lastVolumn);
                }

            }
        };
        timer.schedule(timerTask, 0, 100);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void startRecord() {
        Log.d("AudioRecorder","start getMinBufferSize with parm audioSampleRate:"+audioSampleRate+"audioChannel:"+audioChannel);

        bufferSizeInBytes = AudioRecord.getMinBufferSize(audioSampleRate,
                audioChannel, audioEncode);
        Log.d("AudioRecorder","getMinBufferSize is "+bufferSizeInBytes);
        if (CHANNEL_INDEX_MASK&&android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Log.e("AudioRecorder", "use 4 channel");
            final AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(audioEncode)
                    .setSampleRate(audioSampleRate)
                    .setChannelIndexMask(audioChannel /* 4 channels, 0..3 */)
                    .build();

            audioRecord = new AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setAudioSource(audioInput)
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .build();
        }
        else
            audioRecord = new AudioRecord(audioInput, audioSampleRate, audioChannel, audioEncode, bufferSizeInBytes);
        if (status == Status.STATUS_NO_READY) {
            throw new IllegalStateException("not init");
        }
        if (status == Status.STATUS_START) {
            throw new IllegalStateException("is recording ");
        }
        if(AECenable)
            initAEC();

        Log.d("AudioRecorder", "===startRecord===" + audioRecord.getState());
        audioRecord.startRecording();

        new Thread(new Runnable() {
            @Override
            public void run() {
                recordToFile();
            }
        }).start();
        startTimer();
    }

    public void stop() {
        if (status != Status.STATUS_START) {
            throw new IllegalStateException("not recording");
        } else {
            stopRecorder();
            makeDestFile();
            status = Status.STATUS_READY;
        }
    }

    private void makeDestFile() {
        if (encoder == null)
            return;
        new Thread() {
            @Override
            public void run() {
                encoder.init(audioSampleRate, audioSampleRate * 16 * audioRecord.getChannelCount(), audioRecord.getChannelCount());
                encoder.encode(pcmFileName);
                releaseRecorder();
            }
        }.run();
    }

    public void release() {
        Log.d("AudioRecorder", "===release===");
        stopRecorder();
        releaseRecorder();
        status = Status.STATUS_READY;
        clearFiles();
    }

    private void releaseRecorder() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void stopRecorder() {
        stopTimer();
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearFiles() {
        if (encoder != null) {
            File file = new File(encoder.getDestFile());
            if (file.exists())
                file.delete();
        }
        File pcmfile = new File(pcmFileName);
        if (pcmfile.exists())
            pcmfile.delete();
    }

    private void recordToFile() {
        byte[] audiodata = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            fos = new FileOutputStream(pcmFileName, true);
        } catch (FileNotFoundException e) {
            Log.e("AudioRecorder", e.getMessage());
        }
        Log.i(TAG, "start writeDataTOFile: "+pcmFileName);
        status = Status.STATUS_START;
        while (status == Status.STATUS_START && audioRecord != null) {
            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {

                    //get the volumn  1--10
                    int sum = 0;
                    for (int i = 0; i < readsize; i++) {
                        sum += Math.abs(audiodata[i]);
                    }

//                    if (readsize > 0) {
//                        int raw = sum / readsize;
//                        lastVolumn = raw > 32 ? raw - 32 : 0;
//                        Log.i(TAG, "writeDataTOFile: volumn -- " + raw + " / lastvolumn -- " + lastVolumn);
//                    }


                    if (readsize > 0 && readsize <= audiodata.length) {
                        fos.write(audiodata, 0, readsize);
                        Log.i(TAG, "audiodata.length is"+bufferSizeInBytes+",and readsize is"+readsize);
                    }
                    else
                        Log.i(TAG, "audiodata.length is"+bufferSizeInBytes+",but readsize is"+readsize);
                } catch (IOException e) {
                    Log.e("AudioRecorder", e.getMessage());
                }

            }

        }
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            Log.e("AudioRecorder", e.getMessage());
        }
    }

    public int getCurrentPosition() {
        return currentPosition;
    }


    public Status getStatus() {
        return status;
    }


    public String getVoiceFilePath() {
        return encoder == null ? pcmFileName : encoder.getDestFile();
    }

    public enum Status {
        STATUS_NO_READY,
        STATUS_READY,
        STATUS_START,
        STATUS_PAUSE,
        STATUS_STOP
    }

    public interface CallBack {
        public void recordProgress(int progress);

        public void volumn(int volumn);
    }


    private void initAEC() {
        if (AcousticEchoCanceler.isAvailable()) {
            if (acousticEchoCanceler == null) {
                acousticEchoCanceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
                Log.d(TAG, "initAEC: ---->" + acousticEchoCanceler + "t" + audioRecord.getAudioSessionId());
                if (acousticEchoCanceler == null) {
                    Log.e(TAG, "initAEC: ----->AcousticEchoCanceler create fail.");
                } else {
                    acousticEchoCanceler.setEnabled(true);
                      ;
                }

            }
        }
    }

}