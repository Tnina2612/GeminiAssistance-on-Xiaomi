package com.gemini.guard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
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
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = e.packageName?.toString()
                    ?: e.source?.packageName?.toString()
                    ?: return
                val cls = e.className?.toString()
                    ?: e.source?.className?.toString()
                    ?: ""
                handleWindow(pkg, cls, "state")
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Mot so lop phu (voice overlay) khong phat TYPE_WINDOW_STATE_CHANGED.
                // Doc cua so dang active de khong bo lo.
                val w = try { windows.firstOrNull { it.isActive } } catch (_: Throwable) { null } ?: return
                val root = try { w.root } catch (_: Throwable) { null } ?: return
                val pkg = root.packageName?.toString() ?: return
                if (pkg !in watchedPackages()) return
                val cls = root.className?.toString() ?: ""
                val key = "$pkg|$cls"
                val now = SystemClock.elapsedRealtime()
                if (key == lastWinKey && now - lastWinAt < 1200) return
                lastWinKey = key
                lastWinAt = now
                wlog("windows: $pkg $cls")
                handleWindow(pkg, cls, "windows")
            }
        }
    }

    private var lastWinKey = ""
    private var lastWinAt = 0L

    /** Cac goi co the chua cua so giong noi XiaoAI/YOYO (ke ca proxy). */
    private fun watchedPackages(): List<String> {
        val pr = Restorer.prefs(this)
        return (Filters.split(pr.getString("mi_pkgs", defaultPkgs()) ?: defaultPkgs()) +
            Restorer.targetPkg(this) + GOOGLE_PKG + PROXY_PKGS)
            .filter { it.isNotBlank() }.distinct()
    }

    private fun handleWindow(pkg: String, cls: String, source: String) {
        if (pkg == packageName) return
        // Luon nghe ca Google: chon ChatGPT lam target van can hieu ung khi giu nut nguon mo Gemini.
        // Truoc day Google bi loc khoi packageNames nen am thanh / rung bi mat trong truong hop nay.
        if (pkg == Restorer.GOOGLE_PKG || pkg == Restorer.targetPkg(this)) {
            onAssistantWindow(pkg, cls)
            return
        }
        val pr = Restorer.prefs(this)
        if (!pr.getBoolean("mi_on", false)) return

        remember(pkg, "$pkg  $cls")

        val targets = (Filters.split(pr.getString("mi_pkgs", defaultPkgs()) ?: defaultPkgs()) + PROXY_PKGS).distinct()
        if (pkg !in targets) return

        wlog("Mi AI: $cls" + if (source == "windows") " (windows)" else "")

        // Loc theo ten cua so: chi lop phu giong noi moi goi Gemini, mo app / dich man hinh thi bo qua.
        // Them nhan dang mem: mot so ban ROM dat ten lop khac FrameLayout nhung van la lop phu giong noi.
        val exc = pr.getString("mi_exc", defaultExc()) ?: ""
        val inc = pr.getString("mi_inc", defaultInc()) ?: ""
        val isVoice = inc.isBlank() || Filters.matches(inc, cls) || looksLikeVoice(cls)
        if (exc.isNotBlank() && Filters.matches(exc, cls)) { wlog("→ bỏ qua (thuộc danh sách bỏ qua)"); return }
        if (!isVoice) { wlog("→ bỏ qua (không thuộc danh sách giọng nói)"); return }

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
            // Du phong neu he thong khong bao ket qua cham (giam tu 400ms xuong 180ms)
            handler.postDelayed(fire, tapWait + 180 + delay)
        } else {
            handler.postDelayed({ callGemini(pkg) }, delay)
        }
    }

    /** Nhan dang mem cac lop cua so giong noi khac ten FrameLayout. */
    private fun looksLikeVoice(cls: String): Boolean {
        if (cls.isBlank()) return false
        // Chi xet ten lop ngan: ten day du luon chua "com.xiaomi.voiceassistant" nen
        // neu so khop ca chuoi se khop moi man hinh cai dat cua XiaoAI.
        val simple = cls.substringAfterLast('.')
        return simple.contains("FrameLayout", true) ||
            simple.contains("VoiceOverlay", true) ||
            simple.contains("VoiceAssist", true) ||
            simple.contains("AssistView", true) ||
            simple.contains("Overlay", true) ||
            simple.contains("Float", true)
    }


    private fun callGemini(pkg: String) {
        // Thu dung tien trinh Mi AI (co the khong tac dung neu la tien trinh he thong)
        try { (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).killBackgroundProcesses(pkg) } catch (_: Throwable) {}
        // Chi sua cai dat khi nguoi dung con bat "Canh nen"; tat thi chi goi tro ly, khong ghi he thong.
        if (Restorer.prefs(this).getBoolean("enabled", true)) Restorer.ensure(this)
        val err = Restorer.callTarget(this)
        wlog(if (err == null) "→ gọi xong: OK" else "→ gọi LỖI: $err")
        if (err == null) {
            GeminiSound.play(this)
            verifyGoogleVisible()
        } else {
            // Cho phep lan goi tiep theo thu lai ngay, va thu lai 1 lan sau 350ms.
            lastFire = 0
            handler.postDelayed({
                val retry = Restorer.callTarget(this)
                if (retry == null) {
                    wlog("→ thử lại: OK")
                    GeminiSound.play(this)
                }
            }, 350)
            Toast.makeText(this, L.fmt("Không gọi được %s: %s", Restorer.targetLabel(this), err), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Sau khi goi hidden API, kiem tra xem Google co thuc su hien len khong.
     * Nhieu ROM chan background activity start: API tra ve OK nhung cua so Google
     * khong duoc dua len foreground. Khi do gui ACTION_ASSIST de he thong mo truc tiep.
     */
    private fun verifyGoogleVisible() {
        if (Restorer.targetPkg(this) != Restorer.GOOGLE_PKG) return
        handler.postDelayed({
            val visible = try {
                windows.any { w ->
                    w.isFocused && w.root?.packageName?.toString() == Restorer.GOOGLE_PKG
                }
            } catch (_: Throwable) { false }
            if (!visible) {
                wlog("→ Google chưa hiện, gửi ACTION_ASSIST")
                try {
                    startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Throwable) {
                    wlog("→ ACTION_ASSIST lỗi: ${e.javaClass.simpleName}")
                }
            }
        }, 900)
    }


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

    /** Cua so tro ly muc tieu hoac Google vua hien -> phat am thanh (ca khi giu nut nguon / goi giong noi). */
    private fun onAssistantWindow(pkg: String, cls: String) {
        val pr = Restorer.prefs(this)
        val now = SystemClock.elapsedRealtime()
        val line = "$pkg  $cls"
        if (line != lastAssistantWin) {
            lastAssistantWin = line
            val old = (pr.getString("gseen", "") ?: "").split("\n").filter { it.isNotBlank() && it != line }
            pr.edit().putString("gseen", (listOf(line) + old).take(8).joinToString("\n")).apply()
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

    private var lastSeenTop = ""
    private var lastAssistantWin = ""

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
        if (key == "mi_pkgs" || key == "target_pkg" || key == "tr_pkg") applyInfo()
    }

    /**
     * Binh thuong: chi nhan su kien doi cua so cua goi Mi AI, tro ly muc tieu, Google va app dich.
     * Co "include not important views" chi bat trong luc bam the dich.
     */
    private fun applyInfo(notImportant: Boolean = false) {
        try {
            val pr = Restorer.prefs(this)
            val pk = Filters.split(pr.getString("mi_pkgs", defaultPkgs()) ?: defaultPkgs())
            // Luon gom Google + app dich + goi dich man hinh: Google de nhan hieu ung am thanh
            // ke ca khi target la ChatGPT; goi dich de clickCardByText thay cua so app dich.
            val tr = (pr.getString("tr_pkg", BarOverlay.defaultTrPkg()) ?: "").trim()
            val pkgs = (pk + Restorer.targetPkg(this) + Restorer.GOOGLE_PKG + tr + PROXY_PKGS)
                .filter { it.isNotBlank() }.distinct().toTypedArray()
            val types = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
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

        /** Goi proxy/phu tro cua XiaoAI: co the chua cua so giong noi tren mot so ban ROM. */
        val PROXY_PKGS = listOf("com.miui.voicetrigger", "com.miui.voiceassistProxy")

        /** Goi y theo hang may; nguoi dung van co the sua trong phan cai dat. */
        fun defaultPkgs(): String =
            if (DeviceProfile.isHonor) "com.hihonor.assistant,com.hihonor.magicvoice" else DEFAULT_PKGS
        /** YOYO khong dung FrameLayout lam lop phu giong noi nhu Mi AI, nen de trong = moi cua so. */
        fun defaultInc(): String = if (DeviceProfile.isHonor) "" else DEFAULT_INC
        fun defaultExc(): String = if (DeviceProfile.isHonor) "" else DEFAULT_EXC

        private val IGNORE = setOf(
            "com.android.systemui", "com.miui.home", "com.mi.android.globallauncher",
            "com.hihonor.android.launcher",
            "com.google.android.googlequicksearchbox", "com.android.settings", "android"
        )
    }
}
