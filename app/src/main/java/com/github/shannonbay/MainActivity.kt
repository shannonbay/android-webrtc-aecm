package com.github.shannonbay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.shannonbay.voice.VoicePlayer
import com.github.shannonbay.voice.VoiceRecorder
import com.github.shannonbay.libaecm.AEC
import com.github.shannonbay.libaecm.AEC.AggressiveMode

class MainActivity : AppCompatActivity() {
    private var SAMPLE_RATE = 8000
    private var FRAME_SIZE = 160
    private var playBtn: Button? = null
    private var stopBtn: Button? = null
    private var seekBarSampleRate: SeekBar? = null
    private var textViewSeekBarSampleRate: TextView? = null
    private var textViewSeekBarFrameSizeLabel: TextView? = null
    private var seekBarAggressiveMode: SeekBar? = null
    private var textViewSeekBarAggressiveMode: TextView? = null
    private var seekBarEchoLength: SeekBar? = null
    private var textViewSeekBarEchoLength: TextView? = null
    private val aec = AEC()
    private var voiceRecorder: VoiceRecorder? = null
    private var voicePlayer: VoicePlayer? = null
    private var stop = false
    private var enableAecm = false
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        voiceRecorder = VoiceRecorder()
        voicePlayer = VoicePlayer()
        playBtn = findViewById<Button>(R.id.playBtn)
        playBtn?.setOnClickListener(View.OnClickListener { v: View? -> if (hasRecAudioPermission()) startPlay() })
        stopBtn = findViewById<Button>(R.id.stopBtn)
        stopBtn?.setOnClickListener(View.OnClickListener { v: View? ->
            stopBtn?.setVisibility(View.GONE)
            playBtn?.setVisibility(View.VISIBLE)
            stop()
        })
        val switchAecm = findViewById<Switch>(R.id.switch_aecm)
        switchAecm.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            enableAecm = isChecked
        }
        textViewSeekBarSampleRate =
            findViewById<TextView>(R.id.text_view_seek_bar_sample_rate_label)
        seekBarSampleRate = findViewById<SeekBar>(R.id.seek_bar_sample_rate)
        seekBarSampleRate!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var progress = progress
                if (progress <= 8000) {
                    progress = 0
                    seekBar.progress = progress
                }
                if (progress > 8000) {
                    progress = 16000
                    seekBar.progress = progress
                }
                val s = if (progress == 0) "8000hz" else progress.toString() + "hz"
                textViewSeekBarSampleRate?.setText(s)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val progress = seekBar.progress
                SAMPLE_RATE = if (progress == 0) 8000 else progress
            }
        })
        seekBarSampleRate!!.setProgress(SAMPLE_RATE)
        textViewSeekBarFrameSizeLabel =
            findViewById<TextView>(R.id.text_view_seek_bar_frame_size_label)
        val seekBarFrameSize = findViewById<SeekBar>(R.id.seek_bar_frame_size)
        seekBarFrameSize.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var progress = progress
                if (progress <= 80) {
                    progress = 0
                    seekBar.progress = progress
                }
                if (progress > 80) {
                    progress = 160
                    seekBar.progress = 160
                }
                val s = if (progress == 0) "80" else progress.toString() + ""
                textViewSeekBarFrameSizeLabel?.setText(s)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val progress = seekBar.progress
                FRAME_SIZE = if (progress == 0) 80 else progress
            }
        })
        seekBarFrameSize.progress = FRAME_SIZE
        textViewSeekBarAggressiveMode =
            findViewById<TextView>(R.id.text_view_seek_bar_aggressive_mode_label)
        seekBarAggressiveMode = findViewById<SeekBar>(R.id.seek_bar_aggressive_mode)
        seekBarAggressiveMode!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textViewSeekBarAggressiveMode?.setText(progress.toString() + "")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekBarAggressiveMode!!.setProgress(4)
        textViewSeekBarEchoLength =
            findViewById<TextView>(R.id.text_view_seek_bar_echo_length_label)
        seekBarEchoLength = findViewById<SeekBar>(R.id.seek_bar_echo_length)
        seekBarEchoLength!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress < 1) {
                    seekBarEchoLength?.setProgress(1)
                    return
                }
                textViewSeekBarEchoLength?.setText(progress.toString() + "ms")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekBarEchoLength!!.setProgress(20)
    }

    private fun startPlay() {
        playBtn!!.visibility = View.GONE
        stopBtn!!.visibility = View.VISIBLE
        play()
    }

    @RequiresApi(23)
    private fun hasRecAudioPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true else if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && permissions[0] == Manifest.permission.RECORD_AUDIO) startPlay()
    }

    private fun play() {
        if (enableAecm) aec.setSampFreq(if (seekBarSampleRate!!.progress == 0) AEC.SamplingFrequency.FS_8000Hz else AEC.SamplingFrequency.FS_16000Hz)
        if (enableAecm) aec.setAecmMode(aggressiveMode)
        voiceRecorder!!.start(SAMPLE_RATE, FRAME_SIZE, applicationContext)
        voicePlayer!!.start(SAMPLE_RATE)
        stop = false
        Thread {
            while (!stop) {
                val frame = voiceRecorder!!.frame()
                if (enableAecm) aec.farendBuffer(frame, FRAME_SIZE)
                var resultFrame: ShortArray? = ShortArray(FRAME_SIZE)
                if (enableAecm) resultFrame = aec.echoCancellation(
                    frame,
                    null,
                    FRAME_SIZE.toShort(),
                    seekBarEchoLength!!.progress.toShort()
                )
                voicePlayer!!.write(if (enableAecm) resultFrame else frame)
            }
        }.start()
    }

    private val aggressiveMode: AggressiveMode
        private get() {
            val progress = seekBarAggressiveMode!!.progress
            when (progress) {
                0 -> return AggressiveMode.MILD
                1 -> return AggressiveMode.MEDIUM
                2 -> return AggressiveMode.HIGH
                3 -> return AggressiveMode.AGGRESSIVE
                4 -> return AggressiveMode.MOST_AGGRESSIVE
            }
            return AggressiveMode.AGGRESSIVE
        }

    private fun stop() {
        stop = true
        voiceRecorder!!.release()
        voicePlayer!!.stopPlaying()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
        aec.close() // completely destroys aecm instance, to continue work you need to create a new instance
    }
}