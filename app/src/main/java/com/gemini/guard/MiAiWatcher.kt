package com.gemini.guard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Nhan biet Mi AI / XiaoAi vua bat len (giu nut nguon...) -> tu goi Gemini de len tren.
 * Ghi lai cac package vua thay de ban biet ten goi dung cua Mi AI tren may.
 */
class MiAiWatcher : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastFire = 0L
    private var lastGoogleEvt = 0L

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == Restorer.targetPkg(this)) { onGoogleWindow(e); return }
        val pr = Restorer.prefs(this)
        if (!pr.getBoolean("mi_on", false)) return

        remember(pkg, "$pkg  ${e.className}")

        val targets = (pr.getString("mi_pkgs", DEFAULT_PKGS) ?: DEFAULT_PKGS)
            .split(",", ";", " ", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (pkg !in targets) return

        val cls = e.className?.toString() ?: ""
        wlog("Mi AI: $cls")

        // Che do hoc: ghi lai ten cua so, khong goi Gemini
        val mode = pr.getString("learn_mode", "") ?: ""
        if (mode.isNotEmpty()) {
            if (SystemClock.elapsedRealtime() <= pr.getLong("learn_until", 0L)) { learn(pr, mode, cls); return }
            pr.edit().remove("learn_mode").apply()
        }

        // Loc theo ten cua so: chi lop phu giong noi moi goi Gemini, mo app / dich man hinh thi bo qua
        val exc = pr.getString("mi_exc", DEFAULT_EXC) ?: ""
        val inc = pr.getString("mi_inc", DEFAULT_INC) ?: ""
        if (exc.isNotBlank() && matches(exc, cls)) { wlog("→ bỏ qua (thuộc danh sách bỏ qua)"); return }
        if (inc.isNotBlank() && !matches(inc, cls)) { wlog("→ bỏ qua (không thuộc danh sách giọng nói)"); return }

        val now = SystemClock.elapsedRealtime()
        if (now - lastFire < 4000) { wlog("→ chống lặp (<4s)"); return }   // chong lap
        lastFire = now
        wlog("→ GỌI ${Restorer.targetLabel(this)}")

        val delay = pr.getInt("mi_delay", 350).toLong()
        val doTap = true   // gop chung vao 1 cong tac "Tu goi Gemini"; luon dung ca Back lan cham de chac an

        // Dong Mi AI truoc (Back)
        performGlobalAction(GLOBAL_ACTION_BACK)

        if (doTap) {
            // Cham xong la goi Gemini ngay (Gemini mat ~0.6s moi hien nen Mi AI kip dong xong), khong cho "Mi AI da tat"
            var called = false
            val fire = Runnable {
                if (!called) { called = true; callGemini(pkg) }
            }
            // 0 = cham ngay khi thay Mi AI (nhanh nhat). Tang len neu Mi AI khong dong vi cham qua som.
            val tapWait = pr.getInt("mi_tapwait", 0).toLong().coerceIn(0L, 1000L)
            handler.postDelayed({
                tapOutside(pkg, 0) {
                    handler.postDelayed(fire, delay)
                    // Mi AI dong xong trong ~0.45s sau khi cham. Cho qua moc do ma van con -> cham lai 1 lan bang toa do cai dat
                    handler.postDelayed({ if (miWindowBounds(pkg) != null) tapOutside(pkg, 1, null) }, 600)
                }
            }, tapWait)
            // Du phong neu he thong khong bao ket qua cham
            handler.postDelayed(fire, tapWait + 400 + delay)
        } else {
            handler.postDelayed({ callGemini(pkg) }, delay)
        }
    }


    private fun callGemini(pkg: String) {
        // Thu dung tien trinh Mi AI (co the khong tac dung neu la tien trinh he thong)
        try { (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).killBackgroundProcesses(pkg) } catch (_: Throwable) {}
        Restorer.ensure(this)
        val err = Restorer.callTarget(this)
        wlog(if (err == null) "→ gọi xong: OK" else "→ gọi LỖI: $err")
        if (err == null) GeminiSound.play(this)
        else Toast.makeText(this, L.fmt("Không gọi được %s: %s", Restorer.targetLabel(this), err), Toast.LENGTH_LONG).show()
    }

    /** Khung cua so cua package dang hien tren man hinh (null = khong con). Can canRetrieveWindowContent. */
    private fun miWindowBounds(pkg: String): android.graphics.Rect? {
        return try {
            var best: android.graphics.Rect? = null
            for (w in windows) {
                val root = w.root ?: continue
                if (root.packageName?.toString() == pkg) {
                    val r = android.graphics.Rect(); w.getBoundsInScreen(r)
                    if (best == null || r.width() * r.height() > best.width() * best.height()) best = r
                }
            }
            best
        } catch (_: Throwable) { null }
    }

    /** Cua so cua app Google (Gemini overlay) vua hien -> phat am thanh (ca khi giu nut nguon / goi giong noi). */
    private fun onGoogleWindow(e: AccessibilityEvent) {
        val pr = Restorer.prefs(this)
        val now = SystemClock.elapsedRealtime()
        val cls = e.className?.toString() ?: ""
        if (cls != lastGoogleCls) {
            lastGoogleCls = cls
            val old = (pr.getString("gseen", "") ?: "").split("\n").filter { it.isNotBlank() && it != cls }
            pr.edit().putString("gseen", (listOf(cls) + old).take(8).joinToString("\n")).apply()
        }

        val filter = pr.getString("snd_cls", "")?.trim().orEmpty()
        val gap = pr.getInt("snd_gap_s", 3) * 1000L
        val fresh = now - lastGoogleEvt > gap      // phien moi, khong phai su kien tiep noi
        lastGoogleEvt = now
        if (fresh && pr.getBoolean("snd_on", false) &&
            (filter.isEmpty() || cls.contains(filter, ignoreCase = true)) &&
            now - GeminiSound.lastPlay > gap) GeminiSound.play(this, 0)
        // Rung nhe luc cua so Gemini hien len (cung bo loc + chong lap voi am thanh)
        if (fresh && pr.getBoolean("vib_on", false) &&
            (filter.isEmpty() || cls.contains(filter, ignoreCase = true)) &&
            now - Haptic.last > gap) Haptic.pop(this)
    }

    /** Nhat ky quyet dinh (16 dong moi nhat) - hien trong trang thai de biet vi sao khong goi duoc. */
    private fun wlog(msg: String) {
        try {
            val pr = Restorer.prefs(this)
            val t = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            val old = (pr.getString("wlog", "") ?: "").split("\n").filter { it.isNotBlank() }
            pr.edit().putString("wlog", (listOf("$t $msg") + old).take(16).joinToString("\n")).apply()
        } catch (_: Throwable) {}
    }

    private fun matches(list: String, cls: String): Boolean =
        list.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
            .any { cls.contains(it, ignoreCase = true) }

    private fun learn(pr: SharedPreferences, mode: String, cls: String) {
        if (cls.isEmpty()) return
        val key = if (mode == "inc") "mi_inc" else "mi_exc"
        val cur = (pr.getString(key, "") ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (cls !in cur) pr.edit().putString(key, (cur + cls).joinToString(",")).apply()
        if (mode == "inc") pr.edit().remove("learn_mode").apply()   // giong noi: chi can cua so dau tien
        Toast.makeText(this, L.tr("Đã học (") + L.tr(if (mode == "inc") L.tr("giọng nói") else L.tr("loại trừ")) + "): " + cls, Toast.LENGTH_SHORT).show()
    }

    private var lastSeenTop = ""
    private var lastGoogleCls = ""

    private fun remember(pkg: String, line: String) {
        if (pkg in IGNORE) return
        if (line == lastSeenTop) return          // khong ghi dia lai khi khong doi
        lastSeenTop = line
        val pr = Restorer.prefs(this)
        val old = (pr.getString("seen", "") ?: "").split("\n").filter { it.isNotBlank() && it != line }
        pr.edit().putString("seen", (listOf(line) + old).take(10).joinToString("\n")).apply()
    }

    /**
     * Cham vao cho trong de dong thanh Mi AI.
     * attempt 0: neu lop phu chi chiem 1 phan man hinh -> cham ngay NGOAI khung lop phu.
     * Neu lop phu phu kin man hinh (hoac khong doc duoc) -> dung toa do % cai dat.
     */
    private fun tapOutside(pkg: String, attempt: Int, onDone: (() -> Unit)?) {
        try {
            val pr = Restorer.prefs(this)
            val dm = resources.displayMetrics
            val W = dm.widthPixels.toFloat(); val H = dm.heightPixels.toFloat()
            var x = W * pr.getInt("mi_tx", 8) / 100f
            var y = H * pr.getInt("mi_ty", 10) / 100f
            val r = if (attempt == 0) miWindowBounds(pkg) else null
            if (r != null && r.height() < H * 0.9f) {
                x = W / 2f
                y = if (r.top > H * 0.3f) r.top / 2f          // thanh nam duoi -> cham phia tren
                    else (r.bottom + H) / 2f                  // thanh nam tren -> cham phia duoi
            }
            val path = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
            val cb = object : GestureResultCallback() {
                override fun onCompleted(d: GestureDescription?) { wlog("chạm: hoàn tất"); onDone?.invoke() }
                override fun onCancelled(d: GestureDescription?) { wlog("chạm: bị hủy"); onDone?.invoke() }
            }
            val ok = dispatchGesture(g, cb, null)
            if (!ok) { wlog("chạm: không gửi được"); onDone?.invoke() }
        } catch (_: Throwable) {}
    }

    // ---------- Tiet kiem pin: chi nhan dung su kien can thiet ----------
    private var infoKey = ""
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "mi_pkgs" || key == "target_pkg") applyInfo()
    }

    /**
     * Binh thuong: chi nhan su kien doi cua so cua goi Mi AI + Google (he thong khong day su kien cua app khac).
     * Co "include not important views" chi bat trong luc bam the dich.
     */
    private fun applyInfo(notImportant: Boolean = false) {
        try {
            val pr = Restorer.prefs(this)
            val pk = (pr.getString("mi_pkgs", DEFAULT_PKGS) ?: DEFAULT_PKGS)
                .split(",", ";", " ", "\n").map { it.trim() }.filter { it.isNotEmpty() }
            val pkgs = (pk + Restorer.targetPkg(this)).distinct().toTypedArray()
            val types = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            var flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            if (notImportant) flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            val key = "$types|$flags|${pkgs.joinToString(",")}"
            if (key == infoKey) return
            val info = serviceInfo ?: return
            info.eventTypes = types
            info.flags = flags
            info.packageNames = pkgs
            serviceInfo = info
            infoKey = key
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        if (inst === this) inst = null
        try { Restorer.prefs(this).unregisterOnSharedPreferenceChangeListener(prefListener) } catch (_: Throwable) {}
        super.onDestroy()
    }

    /** Cho den khi the co chu trong `texts` hien trong app `pkg` roi bam vao (toi da timeoutMs). */
    fun clickCardByText(pkg: String, texts: List<String>, timeoutMs: Long, done: (Boolean) -> Unit) {
        applyInfo(true)
        val finish = { ok: Boolean -> applyInfo(false); done(ok) }
        val t0 = SystemClock.elapsedRealtime()
        val r = object : Runnable {
            override fun run() {
                val hit = tryClickText(pkg, texts)
                when {
                    hit != null -> { finish(true) }
                    SystemClock.elapsedRealtime() - t0 > timeoutMs -> { finish(false) }
                    else -> handler.postDelayed(this, 150)
                }
            }
        }
        handler.postDelayed(r, 250)
    }

    private fun tryClickText(pkg: String, texts: List<String>): String? {
        try {
            for (w in windows) {
                val root = w.root ?: continue
                if (root.packageName?.toString() != pkg) continue
                for (t in texts) {
                    for (n in root.findAccessibilityNodeInfosByText(t)) {
                        var c: android.view.accessibility.AccessibilityNodeInfo? = n
                        var depth = 0
                        while (c != null && depth < 8) {
                            if (c.isClickable && c.isEnabled && c.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return t
                            c = c.parent; depth++
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        inst = this
        Restorer.prefs(this).registerOnSharedPreferenceChangeListener(prefListener)
        applyInfo()
        GeminiSound.warm(this)
    }

    override fun onInterrupt() {}

    companion object {
        @Volatile var inst: MiAiWatcher? = null
        const val DEFAULT_PKGS = "com.miui.voiceassist"
        /** Mac dinh san: lop phu giong noi la FrameLayout cua voiceassist; app Mi AI la MainNavigatorActivity -> bo qua */
        const val DEFAULT_INC = "android.widget.FrameLayout"
        const val DEFAULT_EXC = "com.xiaomi.voiceassistant.mainui.navigator.MainNavigatorActivity"
        const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"
        private val IGNORE = setOf(
            "com.android.systemui", "com.miui.home", "com.mi.android.globallauncher",
            "com.google.android.googlequicksearchbox", "com.android.settings", "android"
        )
    }
}
