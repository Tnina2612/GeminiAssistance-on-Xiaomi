package com.gemini.guard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.SystemClock

/**
 * Phat hieu ung am thanh (res/raw/gemini_sound.mp3) khi Gemini hien len.
 * Luon giu san 1 MediaPlayer da prepare de bam la phat ngay (giam tre), roi chuan bi cai ke tiep.
 * Co them LoudnessEnhancer de khuech dai vuot muc 100% cua MediaPlayer.
 */
object GeminiSound {
    @Volatile var lastPlay = 0L
    private val lock = Any()

    private class Ready(val mp: MediaPlayer, val enh: LoudnessEnhancer?)
    private var ready: Ready? = null
    private var preparing = false

    private fun build(app: Context, onReady: (Ready) -> Unit) {
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = app.resources.openRawResourceFd(R.raw.gemini_sound)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnPreparedListener {
                val enh = try { LoudnessEnhancer(it.audioSessionId) } catch (_: Throwable) { null }
                onReady(Ready(it, enh))
            }
            mp.setOnErrorListener { m, _, _ ->
                m.release()
                synchronized(lock) { preparing = false; if (ready?.mp === m) ready = null }
                true
            }
            mp.prepareAsync()
        } catch (_: Throwable) {
            synchronized(lock) { preparing = false }
        }
    }

    /** Chuan bi san 1 player (goi luc service khoi dong va sau moi lan phat). */
    fun warm(ctx: Context) {
        val app = ctx.applicationContext
        if (!Restorer.prefs(app).getBoolean("snd_on", false)) return   // tat am thanh -> khong giu player (tiet kiem pin)
        synchronized(lock) { if (ready != null || preparing) return; preparing = true }
        build(app) { r -> synchronized(lock) { ready = r; preparing = false } }
    }

    /** Nha player dang giu san (khi tat hieu ung am thanh). */
    fun release() {
        val r = synchronized(lock) { val x = ready; ready = null; x } ?: return
        try { r.enh?.release() } catch (_: Throwable) {}
        try { r.mp.release() } catch (_: Throwable) {}
    }

    fun play(ctx: Context, minGapMs: Long = 1500, force: Boolean = false) {
        val app = ctx.applicationContext
        val pr = Restorer.prefs(app)
        if (!force && !pr.getBoolean("snd_on", false)) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPlay < minGapMs) return
        lastPlay = now

        val r = synchronized(lock) { val x = ready; ready = null; x }
        if (r != null) start(r, app) else build(app) { start(it, app) }
        warm(app)
    }

    private fun start(r: Ready, app: Context) {
        val mp = r.mp
        try {
            val pr = Restorer.prefs(app)
            val vol = pr.getInt("snd_vol", 100) / 100f
            val skip = pr.getInt("snd_skip", 0)
            val gainDb = pr.getInt("snd_gain", 0)
            mp.setVolume(vol, vol)
            r.enh?.let {
                try { it.setTargetGain(gainDb * 100); it.enabled = gainDb > 0 } catch (_: Throwable) {}
            }
            mp.setOnCompletionListener {
                try { r.enh?.release() } catch (_: Throwable) {}
                it.release()
            }
            if (skip > 0) {
                mp.setOnSeekCompleteListener { it.start() }
                mp.seekTo(skip)
            } else mp.start()
        } catch (_: Throwable) { try { mp.release() } catch (_: Throwable) {} }
    }
}
