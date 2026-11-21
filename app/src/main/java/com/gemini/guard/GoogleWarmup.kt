package com.gemini.guard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock

/**
 * Giu tien trinh Google `:search` am de lan goi tro ly tiep theo nhanh hon.
 *
 * Google Assistant/Gemini overlay chay trong process `:search` cua Google app.
 * Thu bind vao cac service cung process, exported va khong yeu cau permission.
 * Neu Google doi manifest hoac service khong cho app thuong bind, app bo qua
 * (khong lam hong gi) va thu lai sau 10 phut.
 */
object GoogleWarmup {
    // Uu tien service cua process :search; HotwordService la phuong an du phong.
    private val CANDIDATES = listOf(
        "com.google.android.ssb.service.SsbService",
        "com.google.android.hotword.service.HotwordService"
    )
    private const val RETRY_MS = 10 * 60 * 1000L
    private var conn: ServiceConnection? = null
    private var index = 0
    private var bound = false
    private var lastAttempt = 0L

    fun start(c: Context) {
        if (!Restorer.prefs(c).getBoolean("warm_google", true)) return
        if (conn != null || bound) return
        val now = SystemClock.elapsedRealtime()
        if (lastAttempt != 0L && now - lastAttempt < RETRY_MS) return
        lastAttempt = now
        index = 0
        bindNext(c)
    }

    private fun bindNext(c: Context) {
        if (index >= CANDIDATES.size) {
            Restorer.note(c, "Google warm: không bind được service nào")
            return
        }
        val cls = CANDIDATES[index++]
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (conn !== this) return
                bound = true
                Restorer.note(c, "Google warm: đã kết nối")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (conn !== this) return
                bound = false
                conn = null
                Restorer.note(c, "Google warm: mất kết nối")
            }

            override fun onBindingDied(name: ComponentName?) {
                if (conn !== this) return
                bound = false
                conn = null
            }

            override fun onNullBinding(name: ComponentName?) {
                if (conn !== this) return
                conn = null
                bindNext(c)
            }
        }
        conn = connection
        val i = Intent().setComponent(ComponentName(Restorer.GOOGLE_PKG, cls))
        try {
            if (!c.bindService(i, connection, Context.BIND_AUTO_CREATE)) {
                conn = null
                bindNext(c)
            }
        } catch (e: Throwable) {
            conn = null
            bindNext(c)
        }
    }

    fun stop(c: Context) {
        val connection = conn ?: return
        conn = null
        bound = false
        lastAttempt = 0L
        try { c.unbindService(connection) } catch (_: Throwable) {}
    }

    fun isBound() = bound
}
