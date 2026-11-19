package com.gemini.guard

import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

/** Goi phien tro ly mac dinh (Gemini) qua VoiceInteractionManagerService, giong cach MiCTS lam. */
object VisTrigger {
    /** Tra ve null neu thanh cong, hoac thong bao loi. */
    fun showAssistant(): String? = try {
        val iVims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val stub = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
        val sm = Class.forName("android.os.ServiceManager")
        val binder = HiddenApiBypass.invoke(sm, null, "getService", "voiceinteraction") as IBinder
        val vims = HiddenApiBypass.invoke(stub, null, "asInterface", binder)
        val args = Bundle().apply { putLong("invocation_time_ms", SystemClock.elapsedRealtime()) }
        val m = HiddenApiBypass.getDeclaredMethods(iVims).filterIsInstance<Method>()
            .first { it.name == "showSessionFromSession" }
        val params: Array<Any?> =
            if (m.parameterCount == 4) arrayOf(null, args, 7, null) else arrayOf(null, args, 7)
        HiddenApiBypass.invoke(iVims, vims, "showSessionFromSession", *params)
        null
    } catch (e: Throwable) {
        e.message ?: e.javaClass.simpleName
    }
}
