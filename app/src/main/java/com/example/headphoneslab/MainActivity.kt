
package com.example.headphoneslab

import android.app.Activity
import android.os.Bundle
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Gravity
import android.widget.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

class MainActivity : Activity() {
    private val sampleRate = 44100

    @Volatile private var stopRequested = false
    @Volatile private var frequency = 440.0
    @Volatile private var generation = 0

    private var track: AudioTrack? = null

    private val bg = Color.rgb(0, 0, 0)
    private val panel = Color.rgb(13, 16, 22)
    private val ink = Color.rgb(245, 249, 255)
    private val muted = Color.rgb(143, 158, 178)
    private val blue = Color.rgb(22, 139, 255)
    private val lightBlue = Color.rgb(91, 194, 255)

    private lateinit var status: TextView
    private lateinit var freqLabel: TextView
    private lateinit var visualizer: AudioVisualizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun rounded(color: Int, radius: Int = 18) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(root)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val monogram = TextView(this).apply {
            text = "HL"
            textSize = 27f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(8, 28, 49), 14)
            setPadding(dp(13), dp(8), dp(13), dp(8))
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        brand.addView(TextView(this).apply {
            text = "HEADPHONES"
            textSize = 17f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(ink)
        })

        brand.addView(TextView(this).apply {
            text = "L A B"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(blue)
        })

        header.addView(monogram)
        header.addView(brand)
        add(root, header, 0)

        add(root, label("A U D I O   T E S T   L A B", 11f, blue), 30)

        add(root, label("Tu sonido.\nBajo control.", 30f, ink).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }, 8)

        add(root, label(
            "Herramientas para explorar tus auriculares.",
            14f, muted
        ), 8)

        val frequencyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(panel)
        }

        frequencyCard.addView(label("FRECUENCIA ACTUAL", 11f, muted))

        freqLabel = label("440 Hz", 38f, ink).apply {
            typeface = Typeface.create(
                "sans-serif-light", Typeface.NORMAL
            )
        }
        frequencyCard.addView(freqLabel)

        val slider = SeekBar(this).apply {
            max = 11940
            progress = 380
            progressTintList =
                android.content.res.ColorStateList.valueOf(blue)
            thumbTintList =
                android.content.res.ColorStateList.valueOf(lightBlue)

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        frequency = (60 + progress).toDouble()
                        freqLabel.text = "${frequency.toInt()} Hz"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }

        frequencyCard.addView(slider)
        frequencyCard.addView(
            label("Rango: 60 – 12.000 Hz", 12f, muted)
        )
        add(root, frequencyCard, 24)

        val visualCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
            background = rounded(panel)
        }

        visualCard.addView(
            label("VISUALIZADOR", 11f, muted)
        )

        visualizer = AudioVisualizer(this)
        visualCard.addView(
            visualizer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(112)
            )
        )

        visualCard.addView(
            label("Representación visual del tono generado", 11f, muted)
        )
        add(root, visualCard, 14)

        status = label("●  SISTEMA LISTO", 12f, blue)
        add(root, status, 22)

        add(root, button("▶   Reproducir tono", blue) {
            startAudio("tone")
        }, 12, 54)

        add(root, button("■   Detener audio", Color.rgb(29, 34, 43)) {
            stopAudio()
        }, 9, 50)

        add(root, label("PRUEBA ESTÉREO", 11f, muted), 26)

        val stereoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        stereoRow.addView(
            button("←  Izquierdo", Color.rgb(9, 32, 55)) {
                startAudio("left")
            },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginEnd = dp(5)
            }
        )

        stereoRow.addView(
            button("Derecho  →", Color.rgb(9, 32, 55)) {
                startAudio("right")
            },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginStart = dp(5)
            }
        )

        add(root, stereoRow, 10)

        add(root, button(
            "〰   Barrido de frecuencias · 10 s", panel
        ) {
            startAudio("sweep")
        }, 10, 50)

        val safety = label(
            "Usá volumen bajo y hacé pruebas breves. " +
            "Detené el audio si sentís molestias o zumbidos.\n\n" +
            "El visualizador es ilustrativo: no mide la calidad real " +
            "de los auriculares ni modifica su hardware.",
            12f, muted
        )
        safety.setLineSpacing(dp(3).toFloat(), 1f)
        add(root, safety, 24)

        setContentView(scroll)
    }

    private fun label(value: String, size: Float, color: Int) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
        }

    private fun button(
        value: String,
        color: Int,
        action: () -> Unit
    ) = Button(this).apply {
        text = value
        textSize = 13f
        isAllCaps = false
        setTextColor(ink)
        backgroundTintList =
            android.content.res.ColorStateList.valueOf(color)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setOnClickListener { action() }
    }

    private fun add(
        root: LinearLayout,
        view: View,
        top: Int,
        height: Int = -2
    ) {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (height == -2) LinearLayout.LayoutParams.WRAP_CONTENT
            else dp(height)
        )
        lp.topMargin = dp(top)
        root.addView(view, lp)
    }

    private fun startAudio(mode: String) {
        stopAudio()
        val myGeneration = generation

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBuffer <= 0) {
            status.text = "●  No se pudo iniciar el audio"
            return
        }

        val newTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            status.text = "●  No se pudo iniciar el audio"
            return
        }

        track = newTrack
        stopRequested = false
        visualizer.setActive(true)

        status.text = when (mode) {
            "left" -> "●  Canal izquierdo · 15 s"
            "right" -> "●  Canal derecho · 15 s"
            "sweep" -> "●  Barrido en curso · 10 s"
            else -> "●  Reproduciendo ${frequency.toInt()} Hz · 15 s"
        }

        try {
            newTrack.play()
        } catch (_: Exception) {
            stopAudio()
            status.text = "●  No se pudo reproducir"
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

                    val f = if (mode == "sweep") {
                        60.0 * Math.pow(
                            12000.0 / 60.0,
                            elapsed.toDouble() / limit
                        )
                    } else {
                        frequency
                    }

                    val step = 2.0 * PI * f / sampleRate

                    for (i in 0 until frames) {
                        val sample =
                            (sin(phase) * amplitude).toInt().toShort()

                        when (mode) {
                            "left" -> {
                                buffer[i * 2] = sample
                                buffer[i * 2 + 1] = 0
                            }
                            "right" -> {
                                buffer[i * 2] = 0
                                buffer[i * 2 + 1] = sample
                            }
                            else -> {
                                buffer[i * 2] = sample
                                buffer[i * 2 + 1] = sample
                            }
                        }

                        phase += step
                        if (phase >= 2.0 * PI) phase -= 2.0 * PI
                    }

                    if (newTrack.write(buffer, 0, buffer.size) < 0) break
                }
            } catch (_: Exception) {
            } finally {
                runOnUiThread {
                    if (generation == myGeneration) stopAudio()
                }
            }
        }.start()
    }

    private fun stopAudio() {
        generation += 1
        stopRequested = true
        visualizer.setActive(false)

        val oldTrack = track
        track = null

        try {
            oldTrack?.pause()
            oldTrack?.flush()
            oldTrack?.stop()
        } catch (_: Exception) {
        }

        try {
            oldTrack?.release()
        } catch (_: Exception) {
        }

        status.text = "●  AUDIO APAGADO"
    }

    override fun onStop() {
        stopAudio()
        super.onStop()
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private class AudioVisualizer(context: android.content.Context) :
        View(context) {

        @Volatile private var active = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var tick = 0

        fun setActive(value: Boolean) {
            active = value
            if (value) invalidate()
            else invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val count =  thirtyBars
            val gap = w * 0.018f
            val barWidth = (w - gap * (count - 1)) / count
            val center = h / 2f

            paint.color = Color.rgb(20, 39, 59)
            paint.strokeWidth = 1f
            canvas.drawLine(0f, center, w, center, paint)

            for (i in 0 until count) {
                val wave = abs(
                    sin((i * 0.62) + (tick * 0.16))
                ).toFloat()

                val envelope =
                    0.25f + 0.75f * abs(
                        sin((i - count / 2f) * 0.12)
                    ).toFloat()

                val motion = if (active) wave else 0.18f + wave * 0.12f
                val barHeight = (h * 0.78f * envelope * motion)
                    .coerceIn(dp(4).toFloat(), h * 0.88f)

                val left = i * (barWidth + gap)
                val top = center - barHeight / 2f
                val bottom = center + barHeight / 2f

                paint.color = if (i % 4 == 0) {
                    Color.rgb(91, 194, 255)
                } else {
                    Color.rgb(22, 139, 255)
                }

                canvas.drawRoundRect(
                    left, top, left + barWidth, bottom,
                    barWidth / 2f, barWidth / 2f, paint
                )
            }

            if (active) {
                tick++
                postInvalidateDelayed(45)
            }
        }

        private val thirtyBars = 30

        private fun dp(v: Int): Int =
            (v * resources.displayMetrics.density).toInt()
    }
}
