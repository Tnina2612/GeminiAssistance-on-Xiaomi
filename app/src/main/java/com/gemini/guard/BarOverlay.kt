package com.gemini.guard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class BarOverlay(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun refresh() {
        val on = Restorer.prefs(ctx).getBoolean("bar", false)
        if (on && Settings.canDrawOverlays(ctx)) show() else hide()
    }

    private fun show() {
        hide()
        val p = Restorer.prefs(ctx)
        val dm = ctx.resources.displayMetrics
        val w = dm.widthPixels * p.getInt("bar_w", 40) / 100
        val h = (dm.density * p.getInt("bar_h", 20)).toInt().coerceAtLeast(8)
        val minY = (dm.density * MIN_Y_DP).toInt()
        val maxY = (dm.density * MAX_Y_DP).toInt()
        val alpha = p.getInt("bar_a", 30) * 255 / 100

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // Do dam 0% = vo hinh: khong cho nhan cham de khong chan thao tac day man hinh.
        if (alpha == 0) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        val lp = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            this.y = (dm.density * p.getInt("bar_y", MIN_Y_DP)).toInt().coerceIn(minY, maxY)
        }

        val v = View(ctx)
        v.background = GradientDrawable().apply {
            cornerRadius = h / 2f
            setColor(Color.argb(alpha, 255, 255, 255))
        }
        var downRawY = 0f
        var startY = 0
        var dragging = false
        val slop = android.view.ViewConfiguration.get(ctx).scaledTouchSlop

        val gd = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onLongPress(e: MotionEvent) {
                if (dragging) return
                val a = currentAction(Restorer.prefs(ctx), "bar_lp")
                if (a == "none") return
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                performAction(a)
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (dragging) return false
                val a = currentAction(Restorer.prefs(ctx), "bar_dt")
                if (a != "none") {
                    v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    performAction(a)
                }
                return true
            }
        })
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawY = e.rawY
                    startY = lp.y
                    dragging = false
                    gd.onTouchEvent(e)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging && kotlin.math.abs(e.rawY - downRawY) > slop) {
                        dragging = true
                        // Huy nhan long-press / double-tap dang cho de khong kich hoat nham khi keo.
                        MotionEvent.obtain(e).also { c ->
                            c.action = MotionEvent.ACTION_CANCEL
                            gd.onTouchEvent(c)
                            c.recycle()
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    if (dragging) {
                        lp.y = (startY - (e.rawY - downRawY)).toInt().coerceIn(minY, maxY)
                        try { wm.updateViewLayout(v, lp) } catch (_: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        // Luu vi tri theo dp de giu nguyen khi doi mat do man hinh / xoay may.
                        Restorer.prefs(ctx).edit()
                            .putInt("bar_y", (lp.y / dm.density).toInt().coerceIn(MIN_Y_DP, MAX_Y_DP)).apply()
                        dragging = false
                    } else gd.onTouchEvent(e)
                }
            }
            true
        }

        try {
            wm.addView(v, lp)
            view = v
            // Khi thanh nam sat day, vung vuot he thong (home/back gesture) co the chan thao tac keo.
            // Xin mien tru cu chi cham tren thanh de nguoi dung van keo duoc tu vi tri mac dinh.
            if (Build.VERSION.SDK_INT >= 29) v.post {
                try { v.systemGestureExclusionRects = listOf(Rect(0, 0, v.width, v.height)) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, L.tr("Không hiện được thanh: ") + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    /** Dam bao assistant/voice_interaction_service dang la Google truoc khi goi. Tra ve true neu vua phai sua. */
    private fun preflight(): Boolean {
        if (!Restorer.hasPermission(ctx)) {
            Toast.makeText(ctx,
                L.tr("Chưa cấp WRITE_SECURE_SETTINGS → trợ lý có thể bị sai (hiện bảng chọn). Mở Gemini Guard để lấy lệnh ADB."),
                Toast.LENGTH_LONG).show()
            return false
        }
        return Restorer.ensure(ctx).isNotEmpty()
    }

    /** Thuc hien hanh dong da chon cho cu chi. */
    private fun performAction(action: String) {
        when (action) {
            "gemini" -> launchAssist()
            "micts" -> launchCts()
            "translate" -> launchTranslate()
        }
    }

    private fun launchCts() {
        if (preflight()) {
            // Cho he thong ap dung cai dat roi moi mo MiCTS
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ doLaunchCts() }, 400)
        } else doLaunchCts()
    }

    private fun doLaunchCts() {
        val pkg = Restorer.prefs(ctx).getString("cts_pkg", "com.parallelc.micts") ?: "com.parallelc.micts"
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) {
            Toast.makeText(ctx, L.fmt("Chưa cài MiCTS (%s)", pkg), Toast.LENGTH_LONG).show(); return
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(i) } catch (e: Exception) {
            Toast.makeText(ctx, L.tr("Lỗi mở MiCTS: ") + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchAssist() {
        preflight()
        val err = Restorer.callTarget(ctx)
        if (err == null) GeminiSound.play(ctx)
        else Toast.makeText(ctx, L.fmt("Không gọi được %s: %s", Restorer.targetLabel(ctx), err), Toast.LENGTH_LONG).show()
    }

    /**
     * Bat AI dich man hinh cua Xiaomi: mo app Dich roi de dich vu Tro nang tu bam the "Dich man hinh".
     * (Broadcast truc tiep toi voiceassist khong goi duoc tu app ngoai - da kiem tra bang logcat.)
     */
    /**
     * Mo app dich roi (neu co Tro nang) tu bam the "Dich man hinh". Khong bat Tro nang van
     * dung duoc: chi mo app dich, nguoi dung tu bam the 1 cai - do la gioi han cua Android,
     * khong app thuong nao bam thay nut trong app khac duoc neu khong co Tro nang.
     */
    private fun launchTranslate() {
        val pr = Restorer.prefs(ctx)
        val pkg = (pr.getString("tr_pkg", defaultTrPkg()) ?: "").trim()
        val texts = (pr.getString("tr_text", TR_TEXT) ?: "").split(",", ";", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (pkg.isEmpty()) { Toast.makeText(ctx, L.tr("Chưa nhập tên gói / chữ trên thẻ dịch"), Toast.LENGTH_LONG).show(); return }
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_VIEW).setClassName(pkg, "$pkg.intent.IntentAiTranslateServiceActivity")
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
            Toast.makeText(ctx, L.tr("Không mở được app dịch: ") + e.message, Toast.LENGTH_LONG).show(); return
        }
        val svc = MiAiWatcher.inst
        if (svc == null || texts.isEmpty()) {
            // Khong co Tro nang (hoac chua dien chu tren the): chi mo app, nguoi dung tu bam
            Toast.makeText(ctx, L.tr("Đã mở app dịch – hãy bấm thẻ \"Dịch màn hình\""), Toast.LENGTH_SHORT).show()
            return
        }
        svc.clickCardByText(pkg, texts, 4000L) { ok ->
            if (!ok) Toast.makeText(ctx, L.fmt("Không thấy thẻ \"%s\" trong app dịch", texts.first()), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /** (gia tri, nhan hien thi) */
        val ACTIONS = listOf(
            "gemini" to "Gọi Gemini",
            "micts" to "Mở MiCTS (Circle to Search)",
            "translate" to "Dịch màn hình Xiaomi",
            "none" to "Không làm gì"
        )

        /** Hanh dong cua cu chi `bar_dt` (cham dup) / `bar_lp` (giu). Mac dinh giu nguyen hanh vi cu. */
        fun currentAction(pr: SharedPreferences, key: String): String {
            pr.getString(key, null)?.let { v -> if (ACTIONS.any { it.first == v }) return v }
            return if (key == "bar_dt") "gemini" else "micts"
        }

        const val TR_PKG = "com.xiaomi.aiasst.vision"
        const val TR_TEXT = "Dịch màn hình,Screen translation"

        /** App Dich cua Xiaomi chi co tren may Xiaomi; may khac de trong cho nguoi dung tu nhap. */
        fun defaultTrPkg(): String = if (DeviceProfile.isXiaomi) TR_PKG else ""

        /** Vi tri cao nhat cua thanh (dp) tinh tu day man hinh. */
        const val MAX_Y_DP = 200

        /** Vi tri thap nhat (dp) de thanh nam tren vung cu chi dieu huong cua he thong. */
        const val MIN_Y_DP = 32
    }
}
