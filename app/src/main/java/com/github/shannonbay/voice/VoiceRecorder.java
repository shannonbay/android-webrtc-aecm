package com.github.shannonbay.voice;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class VoiceRecorder {

    private int audioSource = MediaRecorder.AudioSource.DEFAULT;
    private int sampleRate = 8000; // You can change this to your desired sample rate
    private int channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    private AudioRecord recorder;
    private short[] buffer;

    public void start(int sampleRate, int frameSize, Context applicationContext) {


/*        AudioRecord audioRecord = new AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
        );*/
        AudioManager audioManager = ActivityCompat.getSystemService(applicationContext, AudioManager.class);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if(AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler aec = null;
                AcousticEchoCanceler.create(recorder.getAudioSessionId());
                AcousticEchoCanceler.create(recorder.getAudioSessionId());
                AcousticEchoCanceler.create(recorder.getAudioSessionId());
                aec = AcousticEchoCanceler.create(recorder.getAudioSessionId());
                if(aec == null) {
                    Log.e("MYAEC", "NO AEC");
                } else {
                    aec.setEnabled(true);
                    Log.e("MYAEC", "Enabled AEC");
                }
                try {
                    Log.e("MYAEC", System.getProperty("user.dir") + " " + new java.io.File(".").getCanonicalPath());
                } catch (Exception e ) {}
            }
        }


        buffer = new short[frameSize];
        recorder.startRecording();
    }

    public short[] frame() {
        recorder.read(buffer, 0, buffer.length);
        return buffer;
    }

    public void stop() {
        recorder.stop();
    }

    public void release() {
        recorder.release();
    }
}