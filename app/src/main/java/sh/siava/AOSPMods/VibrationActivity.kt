package sh.siava.AOSPMods

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VibrationActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    data class VibrationPattern(val name: String, val pattern: Any)
    data class VibrationEffectData(val name: String, val vibrationEffect: VibrationEffect)

    private val vibrationPatterns = listOf(
        VibrationPattern("Single Short Pulse", 100L),
        VibrationPattern("Single Medium Pulse", 300L),
        VibrationPattern("Single Long Pulse", 500L),
        VibrationPattern("Double Pulse", longArrayOf(0, 100, 200, 100)),
        VibrationPattern("Triple Pulse", longArrayOf(0, 100, 100, 100, 100, 100)),
        VibrationPattern("Heartbeat", longArrayOf(0, 100, 100, 100, 100, 500)),
        VibrationPattern("Alert Pattern", longArrayOf(0, 200, 100, 200)),
        VibrationPattern("Notification", longArrayOf(0, 150, 100, 150)),
        VibrationPattern("Ringtone Pattern", longArrayOf(0, 400, 200, 400, 200, 400)),
        VibrationPattern("Error Pattern", longArrayOf(0, 300, 200, 300, 200, 300, 200, 300)),
        VibrationPattern("Success Pattern", longArrayOf(0, 100, 50, 100)),
        VibrationPattern("Warning Pattern", longArrayOf(0, 200, 50, 200, 50, 200)),
        VibrationPattern("Tick-tock", longArrayOf(0, 50, 200, 50)),
        VibrationPattern(
            "Morse SOS", longArrayOf(
                0,
                100,
                100,
                100,
                100,
                100,
                100,
                300,
                300,
                100,
                300,
                100,
                300,
                300,
                100,
                100,
                100,
                100,
                100,
                100
            )
        ),
        VibrationPattern("Ramp Up", longArrayOf(0, 50, 100, 100, 150, 150, 200)),
        VibrationPattern("Ramp Down", longArrayOf(0, 200, 150, 150, 100, 100, 50)),
        VibrationPattern("Chirp", longArrayOf(0, 30, 20, 30, 20, 30)),
        VibrationPattern("Chirp 2", longArrayOf(0, 60, 20, 60, 20, 60)),
        VibrationPattern("Buzz", 1000L),
        VibrationPattern("Quick Burst", longArrayOf(0, 25, 25, 25, 25, 25, 25, 25, 25)),
        VibrationPattern("Slow Pulse Wave", longArrayOf(0, 400, 600, 400, 600))
    )
    private val vibrationEffects = listOf(
        VibrationEffectData(
            "EFFECT_CLICK", VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        ),
        VibrationEffectData(
            "EFFECT_DOUBLE_CLICK",
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        ),
        VibrationEffectData(
            "EFFECT_HEAVY_CLICK",
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        ),
        VibrationEffectData(
            "EFFECT_TICK", VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vibration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left + 60,
                systemBars.top + 100,
                systemBars.right + 60,
                systemBars.bottom + 100
            )
            insets
        }
        vibrator = (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        setupVibrationButtons()
    }

    private fun setupVibrationButtons() {
        val container = findViewById<LinearLayout>(R.id.buttonsContainer)
        val colorAccent = getColor(android.R.color.system_accent1_600)
        val hex = String.format("#%08X", colorAccent)
        Toast.makeText(this@VibrationActivity, "$colorAccent <-> $hex", Toast.LENGTH_LONG).show()
        vibrationPatterns.forEach { pattern ->
            val button = Button(this).apply {
                text = pattern.name
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(colorAccent)
                setTextColor(getColor(android.R.color.white))
                setOnClickListener {
                    playVibrationPattern(pattern)
                }
            }
            container.addView(button)
        }
        vibrationEffects.forEach { pattern ->
            val button = Button(this).apply {
                text = pattern.name
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(getColor(android.R.color.holo_green_light))
                setTextColor(getColor(android.R.color.white))
                setOnClickListener {
                    vibrator.vibrate(pattern.vibrationEffect)
                }
            }
            container.addView(button)
        }
    }

    private fun playVibrationPattern(pattern: VibrationPattern) {
        when (pattern.pattern) {
            is Long -> {
                val effect = VibrationEffect.createOneShot(
                    pattern.pattern, VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator.vibrate(effect)
            }

            is LongArray -> {
                val effect = VibrationEffect.createWaveform(
                    pattern.pattern, -1 // Don't repeat
                )
                vibrator.vibrate(effect)
            }
        }
    }
}