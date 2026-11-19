package com.gemini.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings

class GuardService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bar: BarOverlay
    private var watching = false

    private val check = Runnable { if (watching) Restorer.ensure(this) }
    private val periodic = object : Runnable {
        override fun run() {
            if (!watching) return
            Restorer.ensure(this@GuardService)
            handler.postDelayed(this, 15 * 60 * 1000L)   // da co ContentObserver bat thay doi ngay, day chi la du phong
        }
    }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (!watching) return
            // Cho HyperOS ghi xong roi moi ghi de
            handler.removeCallbacks(check)
            handler.postDelayed(check, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("guard", "Gemini Guard", NotificationManager.IMPORTANCE_MIN).apply {
                description = L.tr("Trạng thái canh nền và thanh điều hướng")
                setShowBadge(false)
            }
        )
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)

        bar = BarOverlay(this)
        bar.refresh()
        applyWatchMode()
        GeminiSound.warm(this)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        bar.refresh()
        applyWatchMode()
        try {
            getSystemService(NotificationManager::class.java).notify(1, buildNotification())
        } catch (_: Throwable) {}
        if (Restorer.prefs(this).getBoolean("snd_on", false)) GeminiSound.warm(this) else GeminiSound.release()
        return START_STICKY
    }

    /** Bam vao thong bao se mo app thay vi khong lam gi. */
    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val watchingNow = Restorer.prefs(this).getBoolean("enabled", true)
        return Notification.Builder(this, "guard")
            .setContentTitle(L.tr("Gemini Guard đang canh"))
            .setContentText(
                if (watchingNow) L.tr("Đang giữ Gemini làm trợ lý")
                else L.tr("Thanh điều hướng đang bật")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setShowWhen(false)
            .setOngoing(true).build()
    }
    override fun onBind(i: Intent?): IBinder? = null

    /**
     * Chi canh cai dat khi nguoi dung bat "Canh nen" (enabled). Neu chi bat thanh dieu huong
     * (bar) thi service van song de ve thanh, nhung khong duoc phep ghi de cai dat he thong.
     */
    private fun applyWatchMode() {
        val on = Restorer.prefs(this).getBoolean("enabled", true)
        if (on == watching) return
        if (on) {
            watching = true
            try {
                val r = contentResolver
                for (k in listOf("assistant", "voice_interaction_service"))
                    r.registerContentObserver(Settings.Secure.getUriFor(k), false, observer)
                r.registerContentObserver(Settings.Secure.getUriFor(Restorer.ACC_KEY), false, observer)
                r.registerContentObserver(Settings.Global.getUriFor("power_button_long_press"), false, observer)
                handler.post(periodic)
                handler.post(check)
            } catch (_: Throwable) {
                unregister()
            }
        } else {
            unregister()
        }
    }

    private fun unregister() {
        watching = false
        handler.removeCallbacks(check)
        handler.removeCallbacks(periodic)
        try { contentResolver.unregisterContentObserver(observer) } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        bar.hide()
        unregister()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
