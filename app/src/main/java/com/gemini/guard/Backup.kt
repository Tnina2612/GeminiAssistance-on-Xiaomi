package com.gemini.guard

import android.content.Context
import org.json.JSONObject

/**
 * Xuat / nhap cau hinh dang JSON de ho tro tu xa hoac chuyen may.
 * Khong xuat log, trang thai tam thoi, gia tri goc cua nut nguon hay token API.
 */
object Backup {
    private val SKIP = setOf(
        "log", "wlog", "seen", "gseen", "verify_last", "verify_at", "alert_at",
        "last_cycle_boot", "cycle_last", "chatgpt_last", "assist_last",
        "power_orig", "api_token", "onboard_done", "snd_file",
        "bank_paused", "bank_acc_was_on"
    )

    fun exportJson(c: Context): String {
        val root = JSONObject()
        val ver = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (_: Exception) { "?" }
        root.put("app", "GeminiGuard")
        root.put("version", ver)
        root.put("exported_at", System.currentTimeMillis())
        val prefs = JSONObject()
        for ((k, v) in Restorer.prefs(c).all) {
            if (k in SKIP) continue
            when (v) {
                is Boolean, is Int, is Long, is Float, is String -> prefs.put(k, v)
            }
        }
        root.put("prefs", prefs)
        return root.toString(2)
    }

    /** Tra ve so khoa da ap dung; nem loi neu JSON hong. */
    fun importJson(c: Context, text: String): Int {
        val root = JSONObject(text)
        if (root.optString("app") != "GeminiGuard") throw IllegalArgumentException(L.tr("Tệp không phải cấu hình Gemini Guard"))
        val prefs = root.optJSONObject("prefs") ?: throw IllegalArgumentException(L.tr("Tệp thiếu mục prefs"))
        val ed = Restorer.prefs(c).edit()
        var n = 0
        for (k in prefs.keys()) {
            if (k in SKIP) continue
            when (val v = prefs.get(k)) {
                is Boolean -> { ed.putBoolean(k, v); n++ }
                is Int -> { ed.putInt(k, v); n++ }
                is Long -> { ed.putLong(k, v); n++ }
                is Double -> { ed.putFloat(k, v.toFloat()); n++ }
                is String -> { ed.putString(k, v); n++ }
            }
        }
        ed.apply()
        return n
    }
}
