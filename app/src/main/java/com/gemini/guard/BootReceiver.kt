package com.gemini.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pr = Restorer.prefs(c)
        val watching = pr.getBoolean("enabled", true)

        // Chi ghi de cai dat khi nguoi dung con bat "Canh nen".
        if (watching && Restorer.hasPermission(c)) {
            // Khoi dong may: tat -> bat lai Google (nhu Gemini Restore), chay nen vai giay
            if (i.action == Intent.ACTION_BOOT_COMPLETED && pr.getBoolean("cycle_boot", true)) {
                val pending = goAsync()
                Thread {
                    try { Restorer.cycleOnBoot(c.applicationContext) } catch (_: Throwable) {}
                    finally { pending.finish() }
                }.start()
            } else {
                Restorer.ensure(c)
            }
        }

        if (watching) Watchdog.schedule(c) else Watchdog.cancel(c)

        if (watching || pr.getBoolean("bar", false) || pr.getBoolean("bank_guard", false)) {
            try { c.startForegroundService(Intent(c, GuardService::class.java)) } catch (_: Exception) {}
        }
    }
}
