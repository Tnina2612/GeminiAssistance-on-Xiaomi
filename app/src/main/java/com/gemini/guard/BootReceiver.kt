package com.gemini.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pr = Restorer.prefs(c)

        // Khoi dong may: tat -> bat lai Google (nhu Gemini Restore), chay nen vai giay
        if (i.action == Intent.ACTION_BOOT_COMPLETED && pr.getBoolean("cycle_boot", true) && Restorer.hasPermission(c)) {
            val pending = goAsync()
            Thread {
                try { Restorer.cycleOnBoot(c.applicationContext) } catch (_: Throwable) {}
                finally { pending.finish() }
            }.start()
        } else {
            Restorer.ensure(c)
        }

        if (pr.getBoolean("enabled", true) || pr.getBoolean("bar", false)) {
            try { c.startForegroundService(Intent(c, GuardService::class.java)) } catch (_: Exception) {}
        }
    }
}
