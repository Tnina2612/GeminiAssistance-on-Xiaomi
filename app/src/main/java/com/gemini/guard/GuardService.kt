package com.gemini.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import java.util.concurrent.Executors

class GuardService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var bar: BarOverlay
    private var watching = false

    /** Ghi Settings la binder IPC: chay tren luong nen de khong treo main thread. */
    private fun ensureAsync() {
        try {
            worker.execute {
                val changes = try { Restorer.ensure(this) } catch (_: Throwable) { emptyList() }
                if (changes.any { it.contains("NHƯNG") }) handler.post { notifyProblem(changes) }
            }
        } catch (_: Throwable) {}
    }

    /** Bao mot lan/giờ khi hệ thống liên tục đổi ngược cài đặt (app không giữ được). */
    private fun notifyProblem(changes: List<String>) {
        val pr = Restorer.prefs(this)
        val now = System.currentTimeMillis()
        if (now - pr.getLong("alert_at", 0L) < 60 * 60 * 1000L) return
        pr.edit().putLong("alert_at", now).apply()
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "guard_alert")
            .setContentTitle(L.tr("Gemini Guard không giữ được cài đặt"))
            .setContentText(changes.firstOrNull() ?: L.tr("Hệ thống đang tự đổi lại. Bấm để xem chi tiết."))
            .setStyle(Notification.BigTextStyle().bigText(changes.joinToString("\n")))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { getSystemService(NotificationManager::class.java).notify(2, n) } catch (_: Throwable) {}
    }

    private val check = Runnable { if (watching) ensureAsync() }
    private val periodic = object : Runnable {
        override fun run() {
            if (!watching) return
            ensureAsync()
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

    /** Man hinh bat / mo khoa: kiem tra app ngan hang de tam tat Tro nang. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            worker.execute {
                val msg = try { BankGuard.check(this@GuardService) } catch (_: Throwable) { null }
                if (msg != null) Restorer.note(this@GuardService, msg)
            }
        }
    }

    /**
     * Khi bat "tu tat Tro nang cho ngan hang", kiem tra moi 5 giay luc man hinh dang bat.
     * Chi chay khi nguoi dung bat tinh nang nay (mac dinh tat) nen khong ton pin cho may khac.
     */
    private val bankPoll = object : Runnable {
        override fun run() {
            if (!BankGuard.enabled(this@GuardService)) return
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (pm?.isInteractive == true) {
                worker.execute {
                    val msg = try { BankGuard.check(this@GuardService) } catch (_: Throwable) { null }
                    if (msg != null) Restorer.note(this@GuardService, msg)
                }
                handler.postDelayed(this, 5000L)
            } else {
                handler.postDelayed(this, 15000L)
            }
        }
    }

    private fun startBankPoll() {
        handler.removeCallbacks(bankPoll)
        if (BankGuard.enabled(this)) handler.postDelayed(bankPoll, 5000L)
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
        nm.createNotificationChannel(
            NotificationChannel("guard_alert", L.tr("Cảnh báo cài đặt"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = L.tr("Báo khi hệ thống tự đổi lại trợ lý mặc định")
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
        GoogleWarmup.start(this)

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED)
            else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenReceiver, f)
            }
        } catch (_: Throwable) {}
        worker.execute {
            val msg = try { BankGuard.check(this) } catch (_: Throwable) { null }
            if (msg != null) Restorer.note(this, msg)
        }
        startBankPoll()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        bar.refresh()
        applyWatchMode()
        startBankPoll()
        if (Restorer.prefs(this).getBoolean("warm_google", true)) GoogleWarmup.start(this)
        else GoogleWarmup.stop(this)
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
            Watchdog.schedule(this)
            try {
                val r = contentResolver
                for (k in listOf("assistant", "voice_interaction_service"))
                    r.registerContentObserver(Settings.Secure.getUriFor(k), false, observer)
                r.registerContentObserver(Settings.Secure.getUriFor(Restorer.ACC_KEY), false, observer)
                if (DeviceProfile.isXiaomi)
                    r.registerContentObserver(Settings.Global.getUriFor("power_button_long_press"), false, observer)
                handler.post(periodic)
                ensureAsync()
            } catch (_: Throwable) {
                unregister()
            }
        } else {
            Watchdog.cancel(this)
            unregister()
        }
    }

    private fun unregister() {
        watching = false
        handler.removeCallbacks(check)
        handler.removeCallbacks(periodic)
        try { contentResolver.unregisterContentObserver(observer) } catch (_: Throwable) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Bi vuot khoi recents: giu watchdog de tu bat lai, va co gang giu service song.
        if (Restorer.prefs(this).getBoolean("enabled", true)) Watchdog.schedule(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        bar.hide()
        unregister()
        GoogleWarmup.stop(this)
        try { unregisterReceiver(screenReceiver) } catch (_: Throwable) {}
        handler.removeCallbacks(bankPoll)
        handler.removeCallbacksAndMessages(null)
        try { worker.shutdownNow() } catch (_: Throwable) {}
        super.onDestroy()
    }
}
