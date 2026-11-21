package com.gemini.guard

import android.app.Application
import android.content.Context

/** Nap ngon ngu giao dien cho moi thanh phan (Activity, dich vu, receiver). */
class GuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        migrate(this)
        L.load(this)
    }

    /**
     * Chuyen cau hinh cu sang khoa moi mot lan. `dt_translate` (ban cu) -> `bar_dt`,
     * de khong phu thuoc vao khoa da bo.
     */
    private fun migrate(c: Context) {
        val pr = Restorer.prefs(c)
        if (pr.contains("dt_translate")) {
            val legacy = pr.getBoolean("dt_translate", false)
            val ed = pr.edit()
            if (!pr.contains("bar_dt")) ed.putString("bar_dt", if (legacy) "translate" else "gemini")
            ed.remove("dt_translate")
            ed.apply()
        }
    }
}
