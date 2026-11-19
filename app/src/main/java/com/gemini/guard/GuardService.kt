package com.gemini.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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

    private val check = Runnable { Restorer.ensure(this) }
    private val periodic = object : Runnable {
        override fun run() {
            Restorer.ensure(this@GuardService)
            handler.postDelayed(this, 15 * 60 * 1000L)   // da co ContentObserver bat thay doi ngay, day chi la du phong
        }
    }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            // Cho HyperOS ghi xong roi moi ghi de
            handler.removeCallbacks(check)
            handler.postDelayed(check, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("guard", "Gemini Guard", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, "guard")
            .setContentTitle(L.tr("Gemini Guard dang canh"))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)

        val r = contentResolver
        for (k in listOf("assistant", "voice_interaction_service"))
            r.registerContentObserver(Settings.Secure.getUriFor(k), false, observer)
        r.registerContentObserver(Settings.Secure.getUriFor(Restorer.ACC_KEY), false, observer)
        r.registerContentObserver(Settings.Global.getUriFor("power_button_long_press"), false, observer)
        handler.post(periodic)
        GeminiSound.warm(this)
        bar = BarOverlay(this)
        bar.refresh()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        bar.refresh()
        if (Restorer.prefs(this).getBoolean("snd_on", false)) GeminiSound.warm(this) else GeminiSound.release()
        return START_STICKY
    }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        bar.hide()
        contentResolver.unregisterContentObserver(observer)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
