package com.gemini.guard

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Watchdog tang 2: JobScheduler chay dinh ky ke ca khi GuardService bi HyperOS kill.
 * Moi lan chay: sua cai dat (neu con bat "Canh nen") va thu khoi dong lai GuardService.
 */
class WatchdogJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val c = applicationContext
        if (!Restorer.prefs(c).getBoolean("enabled", true)) {
            Watchdog.cancel(c)
            return false
        }
        Thread {
            try { Restorer.ensure(c) } catch (_: Throwable) {}
            try {
                c.startForegroundService(Intent(c, GuardService::class.java))
            } catch (_: Throwable) {}
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

object Watchdog {
    private const val JOB_ID = 1001
    private const val PERIOD_MS = 15 * 60 * 1000L

    /** Lap lich dinh ky (khong lam gi neu da co). */
    fun schedule(c: Context) {
        val js = c.getSystemService(JobScheduler::class.java) ?: return
        try {
            if (js.getPendingJob(JOB_ID) != null) return
            val info = JobInfo.Builder(JOB_ID, ComponentName(c, WatchdogJob::class.java))
                .setPeriodic(PERIOD_MS)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setRequiresCharging(false)
                .build()
            js.schedule(info)
        } catch (_: Throwable) {}
    }

    fun cancel(c: Context) {
        try { c.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID) } catch (_: Throwable) {}
    }
}
