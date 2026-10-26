package com.example.headphoneslab

import android.app.Activity
import android.os.Bundle
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : Activity() {
    private val sampleRate = 44100
    @Volatile private var stopRequested = false
    @Volatile private var frequency = 440.0
    @Volatile private var generation = 0
    private var track: AudioTrack? = null
    private val bg = Color.rgb(17, 19, 24)
    private val panel = Color.rgb(29, 33, 42)
    private val ink = Color.rgb(241, 245, 249)
    private val muted = Color.rgb(164, 174, 192)
    private val accent = Color.rgb(167, 139, 250)
    private lateinit var status: TextView
    private lateinit var freqLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(20))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        val title = TextView(this).apply {
            text = "Headphones Lab"
            textSize = 28f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val subtitle = label("Un pequeño laboratorio de audio", 14f, muted)
        status = label("Listo para probar", 15f, accent)
        freqLabel = label("Frecuencia: 440 Hz", 18f, ink)
        val slider = SeekBar(this).apply {
            max = 11940
            progress = 380
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    frequency = (60 + progress).toDouble()
                    freqLabel.text = "Frecuencia: ${frequency.toInt()} Hz"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        add(root, title, 0)
        add(root, subtitle, 6)
        add(root, status, 22)
        add(root, freqLabel, 16)
        add(root, slider, 4)
        add(root, label("Deslizá entre 60 y 12.000 Hz.", 12f, muted), 0)
        add(root, button("▶  Reproducir tono") { startAudio("tone") }, 20, 52)
        add(root, button("■  Detener audio") { stopAudio() }, 8, 48)
        add(root, button("←  Probar canal izquierdo") { startAudio("left") }, 18, 48)
        add(root, button("Probar canal derecho  →") { startAudio("right") }, 8, 48)
        add(root, button("〰  Barrido de frecuencias") { startAudio("sweep") }, 18, 48)
        add(root, label("Empezá con volumen bajo. Si sentís molestia, zumbido o mareo, detené la prueba.", 13f, muted), 24)
        add(root, label("No añade ANC ni modifica el hardware de los auriculares.", 12f, muted), 12)
        setContentView(scroll)
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        textSize = 14f
        isAllCaps = false
        setTextColor(ink)
        backgroundTintList = android.content.res.ColorStateList.valueOf(panel)
        setOnClickListener { action() }
    }

    private fun add(root: LinearLayout, view: View, top: Int, height: Int = -2) {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (height == -2) LinearLayout.LayoutParams.WRAP_CONTENT else dp(height)
        )
        lp.topMargin = dp(top)
        root.addView(view, lp)
    }

    private fun startAudio(mode: String) {
        stopAudio()
        val myGeneration = generation
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            status.text = "No se pudo iniciar el audio"
            return
        }
        val newTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            status.text = "No se pudo iniciar el audio"
            return
        }
        track = newTrack
        stopRequested = false
        status.text = when (mode) {
            "left" -> "Probando canal izquierdo (15 s)"
            "right" -> "Probando canal derecho (15 s)"
            "sweep" -> "Barrido en curso (10 s)"
            else -> "Reproduciendo ${frequency.toInt()} Hz (15 s)"
        }
        try { newTrack.play() } catch (_: Exception) {
            stopAudio()
            status.text = "No se pudo reproducir"
            return
        }
        Thread {
            val frames = 1024
            val buffer = ShortArray(frames * 2)
            var phase = 0.0
            val start = System.currentTimeMillis()
            val limit = if (mode == "sweep") 10_000L else 15_000L
            val amplitude = 0.05 * Short.MAX_VALUE
            try {
                while (!stopRequested && generation == myGeneration) {
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed >= limit) break
                    val f = if (mode == "sweep") 60.0 * Math.pow(12000.0 / 60.0, elapsed.toDouble() / limit) else frequency
                    val step = 2.0 * PI * f / sampleRate
                    for (i in 0 until frames) {
                        val sample = (sin(phase) * amplitude).toInt().toShort()
                        when (mode) {
                            "left" -> { buffer[i * 2] = sample; buffer[i * 2 + 1] = 0 }
                            "right" -> { buffer[i * 2] = 0; buffer[i * 2 + 1] = sample }
                            else -> { buffer[i * 2] = sample; buffer[i * 2 + 1] = sample }
                        }
                        phase += step
                        if (phase >= 2.0 * PI) phase -= 2.0 * PI
                    }
                    if (newTrack.write(buffer, 0, buffer.size) < 0) break
                }
            } catch (_: Exception) {
            } finally {
                runOnUiThread { if (generation == myGeneration) stopAudio() }
            }
        }.start()
    }

    private fun stopAudio() {
        generation += 1
        stopRequested = true
        try { track?.pause(); track?.flush(); track?.stop(); track?.release() } catch (_: Exception) {}
        track = null
        status.text = "Audio apagado"
    }

    override fun onStop() { stopAudio(); super.onStop() }
    override fun onDestroy() { stopAudio(); super.onDestroy() }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
