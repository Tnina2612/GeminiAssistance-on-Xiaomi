package com.gemini.guard

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Rung nhe kieu "pop" khi Gemini hien len: 1 nhip manh ngan + 1 nhip nhe hon theo sau.
 * Dung usage ALARM de he thong khong chan khi app chay nen / dang tat rung cam ung.
 */
object Haptic {
    @Volatile var last = 0L

    fun pop(ctx: Context, force: Boolean = false) {
        val pr = Restorer.prefs(ctx)
        if (!force && !pr.getBoolean("vib_on", false)) return
        val strength = pr.getInt("vib_str", 60).coerceIn(5, 100)
        val dur = pr.getInt("vib_ms", 25).coerceIn(5, 100)
        last = SystemClock.elapsedRealtime()
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31)
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (!v.hasVibrator()) return
            val tail = (dur * 2 / 3).coerceAtLeast(5)
            val timings = longArrayOf(0, dur.toLong(), 35, tail.toLong())
            val effect = if (v.hasAmplitudeControl()) {
                val amp = (255 * strength / 100).coerceIn(1, 255)
                VibrationEffect.createWaveform(timings, intArrayOf(0, amp, 0, (amp / 2).coerceAtLeast(1)), -1)
            } else VibrationEffect.createWaveform(timings, -1)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            v.vibrate(effect, attrs)
        } catch (_: Throwable) {}
    }
}
