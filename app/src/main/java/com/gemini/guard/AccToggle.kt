package com.gemini.guard

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Bat/tat rieng dich vu Tro nang cua Gemini Guard bang tay (dung chung cho o Control Center va phim tat app). */
object AccToggle {
    private fun isOurs(c: Context, entry: String): Boolean =
        ComponentName.unflattenFromString(entry.trim()) == ComponentName(c, MiAiWatcher::class.java)

    fun isOn(c: Context): Boolean =
        (Settings.Secure.getString(c.contentResolver, Restorer.ACC_KEY) ?: "").split(":").any { isOurs(c, it) }

    /** Bat/tat rieng dich vu Gemini Guard, giu nguyen moi dich vu Tro nang khac dang bat. */
    fun setEnabled(c: Context, enable: Boolean): Boolean = try {
        val r = c.contentResolver
        val comp = ComponentName(c, MiAiWatcher::class.java).flattenToString()
        val cur = (Settings.Secure.getString(r, Restorer.ACC_KEY) ?: "").split(":").filter { it.isNotBlank() }.toMutableList()
        val has = cur.any { isOurs(c, it) }
        if (enable && !has) cur.add(comp)
        if (!enable) cur.removeAll { isOurs(c, it) }
        Settings.Secure.putString(r, Restorer.ACC_KEY, cur.joinToString(":"))
        if (enable) Settings.Secure.putInt(r, "accessibility_enabled", 1)
        true
    } catch (_: Throwable) { false }

    /** Dao trang thai. Khi tat bang tay se danh dau acc_tile_off de Restorer / canh nen khong tu bat lai. */
    fun toggle(c: Context): String {
        if (!Restorer.hasPermission(c)) return L.tr("Chưa cấp quyền WRITE_SECURE_SETTINGS (cấp bằng ADB trước).")
        val pr = Restorer.prefs(c)
        return if (isOn(c)) {
            pr.edit().putBoolean("acc_tile_off", true).apply()
            if (setEnabled(c, false)) {
                L.tr("Đã tắt Trợ năng Gemini Guard")
            } else {
                pr.edit().putBoolean("acc_tile_off", false).apply()
                L.tr("Không tắt được Trợ năng")
            }
        } else {
            pr.edit().putBoolean("acc_tile_off", false).apply()
            if (setEnabled(c, true)) L.tr("Đã bật Trợ năng Gemini Guard") else L.tr("Không bật được Trợ năng")
        }
    }
}
