package com.gemini.guard

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Goi phien tro ly that (Gemini overlay) qua VoiceInteractionManagerService. null = thanh cong. */
object GeminiCall {
    private fun invoke(): Any? {
        // Gioi han hidden API chi co tu Android 9 (API 28). Android 8.0/8.1 khong can bypass.
        if (Build.VERSION.SDK_INT >= 28) HiddenApiBypass.addHiddenApiExemptions("")
        val sm = Class.forName("android.os.ServiceManager")
        val binder = sm.getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as IBinder
        val stub = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
        val vims = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        val iface = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val m = iface.methods.first { it.name == "showSessionFromSession" }
        val args = Bundle().apply { putLong("invocation_time_ms", SystemClock.elapsedRealtime()) }
        // Dung theo kieu tham so thay vi dem so luong: chu ky ham nay doi qua cac ban Android
        // (them session token / String...). Tham so la khong ro thi truyen null.
        val params: Array<Any?> = m.parameterTypes.map { t ->
            when {
                t == Bundle::class.java -> args
                t == Int::class.javaPrimitiveType -> 7
                else -> null
            }
        }.toTypedArray()
        return m.invoke(vims, *params)
    }

    /** Giu nguyen hanh vi cu (Google): chi bao loi khi co ngoai le. */
    fun show(): String? = try { invoke(); null } catch (e: Throwable) { (e.cause ?: e).toString() }

    /** Dung cho tro ly khac (ChatGPT): coi gia tri tra ve false la that bai de con thu cach khac. */
    fun showChecked(): String? = try {
        val r = invoke()
        if (r is Boolean && !r) "showSessionFromSession=false (hệ thống không mở phiên trợ lý)" else null
    } catch (e: Throwable) { (e.cause ?: e).toString() }
}
