package com.gemini.guard

import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Goi phien tro ly that (Gemini overlay) qua VoiceInteractionManagerService. null = thanh cong. */
object GeminiCall {
    private fun invoke(): Any? {
        HiddenApiBypass.addHiddenApiExemptions("")
        val sm = Class.forName("android.os.ServiceManager")
        val binder = sm.getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as IBinder
        val stub = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
        val vims = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        val iface = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val m = iface.methods.first { it.name == "showSessionFromSession" }
        val args = Bundle().apply { putLong("invocation_time_ms", SystemClock.elapsedRealtime()) }
        val params: Array<Any?> =
            if (m.parameterCount == 4) arrayOf(null, args, 7, null) else arrayOf(null, args, 7)
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
