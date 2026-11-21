package com.gemini.guard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Tu tat Tro nang khi nguoi dung mo app ngan hang / VNeID (cac app nay chan Tro nang),
 * roi bat lai khi roi khoi app do. Can quyen "Truy cap su dung" (Usage access).
 * Mac dinh TAT; nguoi dung tu nhap danh sach ten goi.
 */
object BankGuard {
    fun hasAccess(c: Context): Boolean = try {
        val ops = c.getSystemService(AppOpsManager::class.java)
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) ==
            AppOpsManager.MODE_ALLOWED
    } catch (_: Throwable) { false }

    fun enabled(c: Context) = Restorer.prefs(c).getBoolean("bank_guard", false)

    fun packages(c: Context): List<String> =
        Filters.split(Restorer.prefs(c).getString("bank_pkgs", "") ?: "")

    /** Ten goi cua app dang o tien canh (dua tren su kien UsageStats gan nhat). */
    fun foreground(c: Context): String? {
        if (!hasAccess(c)) return null
        return try {
            val usm = c.getSystemService(UsageStatsManager::class.java) ?: return null
            val now = System.currentTimeMillis()
            val ev = usm.queryEvents(now - 10 * 60_000, now) ?: return null
            var last: String? = null
            val e = UsageEvents.Event()
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e)
                @Suppress("DEPRECATION")
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
            }
            last
        } catch (_: Throwable) { null }
    }

    /** Goi khi man hinh bat / user mo khoa. Tra ve mo ta neu vua thay doi Tro nang. */
    fun check(c: Context): String? {
        val pr = Restorer.prefs(c)
        // Tat cong tac trong luc dang tam tat -> tra Tro nang lai neu truoc do dang bat.
        if (!enabled(c)) {
            if (pr.getBoolean("bank_paused", false)) {
                val wasOn = pr.getBoolean("bank_acc_was_on", false)
                pr.edit().putBoolean("bank_paused", false).remove("bank_acc_was_on").apply()
                if (wasOn && !pr.getBoolean("acc_tile_off", false)) {
                    AccToggle.setEnabled(c, true)
                    return L.tr("Đã bật lại Trợ năng sau khi tắt tự tắt cho ngân hàng")
                }
            }
            return null
        }
        if (!hasAccess(c)) return null
        val fg = foreground(c) ?: return null
        val banks = packages(c)
        if (banks.isEmpty()) {
            if (pr.getBoolean("bank_paused", false)) {
                val wasOn = pr.getBoolean("bank_acc_was_on", false)
                pr.edit().putBoolean("bank_paused", false).remove("bank_acc_was_on").apply()
                if (wasOn && !pr.getBoolean("acc_tile_off", false)) AccToggle.setEnabled(c, true)
            }
            return null
        }
        val inBank = banks.any { fg == it || fg.startsWith("$it.") }
        val paused = pr.getBoolean("bank_paused", false)

        if (inBank && !paused) {
            val wasOn = Restorer.accessibilityOn(c)
            if (wasOn && AccToggle.setEnabled(c, false)) {
                pr.edit().putBoolean("bank_paused", true).putBoolean("bank_acc_was_on", true).apply()
                return L.fmt("Đã tắt Trợ năng vì mở %s", fg)
            }
            return null
        }
        if (!inBank && paused) {
            val wasOn = pr.getBoolean("bank_acc_was_on", false)
            pr.edit().putBoolean("bank_paused", false).remove("bank_acc_was_on").apply()
            // Khong tu bat lai neu nguoi dung da tat tay bang phim tat.
            if (wasOn && !pr.getBoolean("acc_tile_off", false)) {
                AccToggle.setEnabled(c, true)
                return L.tr("Đã bật lại Trợ năng sau khi rời app ngân hàng")
            }
        }
        return null
    }
}
