package com.gemini.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * API dieu khien tu ben ngoai (Tasker, MacroDroid, adb...).
 *
 * Bao mat: phai gui kem extra "token" dung voi ma trong phan Cong cu.
 * Chua tao ma (rong) = API tat. Khong dung permission signature vi Tasker khong giu duoc.
 *
 * Vi du adb:
 *   adb shell am broadcast -a com.gemini.guard.ACTION_ASSIST \
 *     -n com.gemini.guard/.CommandReceiver --es token <ma>
 */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pr = Restorer.prefs(c)
        val expected = pr.getString("api_token", "")?.trim().orEmpty()
        if (expected.isEmpty()) return
        val given = i.getStringExtra("token")?.trim().orEmpty()
        if (!same(expected, given)) return

        when (i.action) {
            ACTION_ASSIST -> {
                val pending = goAsync()
                Thread {
                    try {
                        if (pr.getBoolean("enabled", true)) Restorer.ensure(c)
                        Restorer.callTarget(c)
                    } catch (_: Throwable) {
                    } finally { pending.finish() }
                }.start()
            }
            ACTION_BAR_ON -> {
                pr.edit().putBoolean("bar", true).apply()
                try { c.startForegroundService(Intent(c, GuardService::class.java)) } catch (_: Throwable) {}
            }
            ACTION_BAR_OFF -> {
                pr.edit().putBoolean("bar", false).apply()
                try { c.startService(Intent(c, GuardService::class.java)) } catch (_: Throwable) {}
            }
            ACTION_TOGGLE_ACC -> AccToggle.toggle(c)
        }
    }

    /** So sanh khong phu thuoc thoi gian (tranh do dai token lo ra qua timing). */
    private fun same(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty() || a.length != b.length) return false
        var d = 0
        for (n in a.indices) d = d or (a[n].code xor b[n].code)
        return d == 0
    }

    companion object {
        const val ACTION_ASSIST = "com.gemini.guard.ACTION_ASSIST"
        const val ACTION_BAR_ON = "com.gemini.guard.ACTION_BAR_ON"
        const val ACTION_BAR_OFF = "com.gemini.guard.ACTION_BAR_OFF"
        const val ACTION_TOGGLE_ACC = "com.gemini.guard.ACTION_TOGGLE_ACC"

        fun newToken(): String {
            val bytes = ByteArray(8)
            java.security.SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
