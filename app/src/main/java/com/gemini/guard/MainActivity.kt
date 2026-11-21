package com.gemini.guard

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.widget.ImageView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

private val BG = 0xFFF1F3F8.toInt()
private val CARD = Color.WHITE
private val INK = 0xFF1B1D22.toInt()
private val MUTED = 0xFF6B7280.toInt()
private val ACCENT = 0xFF3B6FE0.toInt()
private val TONAL = 0xFFE6EDFC.toInt()
private val GOOD = 0xFF1E8E3E.toInt()
private val BAD = 0xFFC62828.toInt()
private val FIELD = 0xFFF4F5F9.toInt()
private val LINE = 0xFFD9DCE5.toInt()

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var ctsPkg: EditText
    private lateinit var miPkgs: EditText
    private lateinit var miInc: EditText
    private lateinit var miExc: EditText
    private lateinit var trPkg: EditText
    private lateinit var trTxt: EditText
    private lateinit var sndCls: EditText
    private lateinit var customTarget: EditText
    private lateinit var bankPkgs: EditText
    private lateinit var warnCard: LinearLayout
    private lateinit var stateBox: LinearLayout

    private val REQ_EXPORT = 10
    private val REQ_IMPORT = 11
    private val REQ_SOUND = 12

    private fun prefs() = Restorer.prefs(this)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun pushService() {
        val p = prefs()
        val watching = p.getBoolean("enabled", true)
        if (!p.getBoolean("bank_guard", false) && p.getBoolean("bank_paused", false)) {
            val wasOn = p.getBoolean("bank_acc_was_on", false)
            p.edit().putBoolean("bank_paused", false).remove("bank_acc_was_on").apply()
            if (wasOn && !p.getBoolean("acc_tile_off", false)) AccToggle.setEnabled(this, true)
        }
        if (watching) {
            Restorer.ensure(this)
            Watchdog.schedule(this)
        } else Watchdog.cancel(this)
        if (watching || p.getBoolean("bar", false) || p.getBoolean("bank_guard", false))
            startForegroundService(Intent(this, GuardService::class.java))
        else stopService(Intent(this, GuardService::class.java))
    }

    // ---------- Hỗ trợ ----------
    private fun openUrl(u: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: Exception) { Toast.makeText(this, L.tr("Không mở được liên kết: ") + e.message, Toast.LENGTH_LONG).show() }
    }

    /** Luu anh QR vao thu vien (Pictures/GeminiGuard) de quet bang ung dung ngan hang tren cung dien thoai. */
    private fun saveQr() {
        try {
            if (android.os.Build.VERSION.SDK_INT < 29) {
                Toast.makeText(this, L.tr("Android thấp: hãy chụp màn hình mã QR rồi quét từ thư viện"), Toast.LENGTH_LONG).show(); return
            }
            val bmp = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.qr_donate)
            val cv = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "GeminiGuard_QR_mi_tom.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/GeminiGuard")
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                ?: throw IllegalStateException(L.tr("không tạo được tệp"))
            contentResolver.openOutputStream(uri)?.use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, L.tr("Đã lưu ảnh QR vào thư viện (Pictures/GeminiGuard)"), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, L.tr("Không lưu được ảnh: ") + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSupport() {
        val url = "https://www.facebook.com/datbeans512/"
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        body.addView(label(L.tr("Cần hỗ trợ cài đặt hoặc gặp lỗi? Liên hệ admin Gemini Guard qua Facebook để được trợ giúp."), 14f, INK))
        body.addView(label("Facebook admin", 13f, MUTED, true).apply { setPadding(0, dp(14), 0, 0) })
        body.addView(TextView(this).apply {
            text = url; textSize = 14f; setTextColor(ACCENT)
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setPadding(0, dp(4), 0, dp(4))
            setOnClickListener { openUrl(url) }
        })
        body.addView(label(
            L.tr("Khi nhờ hỗ trợ, bạn nên gửi kèm ảnh chụp màn hình và nút \"Sao chép trạng thái\" ") +
                L.tr("(mục Công cụ và chẩn đoán)."),
            12f, MUTED).apply { setPadding(0, dp(12), 0, 0) })

        // Ung ho admin
        body.addView(View(this).apply { setBackgroundColor(0x1F000000) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(18) })
        body.addView(label(L.tr("Hỗ trợ admin gói mì tôm"), 16f, INK, true).apply { setPadding(0, dp(16), 0, 0) })
        body.addView(label(L.tr("Nếu thấy app hữu ích, bạn có thể mời admin một gói mì tôm bằng cách quét mã QR bên dưới (mọi ứng dụng ngân hàng và ví đều quét được). Không bắt buộc nhé."), 13f, MUTED)
            .apply { setPadding(0, dp(6), 0, dp(10)) })
        body.addView(ImageView(this).apply {
            setImageResource(R.drawable.qr_donate)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        btn(body, L.tr("Lưu ảnh QR vào thư viện"), top = 8) { saveQr() }
        body.addView(View(this), LinearLayout.LayoutParams(1, dp(8)))

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle(L.tr("Hỗ trợ"))
            .setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton(L.tr("Mở Facebook")) { _, _ -> openUrl(url) }
            .setNeutralButton(L.tr("Sao chép link")) { _, _ -> copy("facebook", url, L.tr("Đã sao chép liên kết")) }
            .setNegativeButton(L.tr("Đóng"), null)
            .show()
    }

    // ---------- Thành phần giao diện ----------
    private fun shape(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun label(t: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = sp; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun card(root: LinearLayout, title: String, desc: String? = null): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(CARD, 18)
            setPadding(dp(18), dp(16), dp(18), dp(18))
            elevation = dp(1).toFloat()
        }
        c.addView(label(title, 17f, INK, true))
        if (desc != null) c.addView(label(desc, 13f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
        root.addView(c, lp(14))
        return c
    }

    private fun sw(p: LinearLayout, text: String, hint: String?, key: String, def: Boolean): Switch {
        val s = Switch(this).apply {
            this.text = text; textSize = 15f; setTextColor(INK)
            isChecked = prefs().getBoolean(key, def)
            setOnCheckedChangeListener { _, on ->
                prefs().edit().putBoolean(key, on).apply()
                // Tắt ghi đè nút nguồn -> trả lại ngay giá trị gốc (menu nguồn).
                if (key == "power_key" && !on) {
                    Toast.makeText(this@MainActivity, Restorer.restorePowerKey(this@MainActivity), Toast.LENGTH_LONG).show()
                }
                pushService(); refresh()
            }
        }
        p.addView(s, lp(14))
        if (hint != null) p.addView(label(hint, 12f, MUTED).apply { setPadding(0, dp(2), 0, 0) }, lp(0))
        return s
    }

    /** Chon ngon ngu giao dien: tu dong / English / Tieng Viet. */
    private fun langRow(p: LinearLayout) {
        val opts = listOf("auto" to "Auto / Tự động", "en" to "English", "vi" to "Tiếng Việt")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        row.addView(label("Language / Ngôn ngữ", 14f, INK), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val cur = prefs().getString("lang", "auto") ?: "auto"
        row.addView(label(opts.firstOrNull { it.first == cur }?.second ?: opts[0].second, 14f, ACCENT, true))
        row.setOnClickListener {
            val idx = opts.indexOfFirst { it.first == cur }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("Language / Ngôn ngữ")
                .setSingleChoiceItems(opts.map { it.second }.toTypedArray(), idx) { d, which ->
                    prefs().edit().putString("lang", opts[which].first).apply()
                    L.load(this)
                    d.dismiss()
                    recreate()
                }
                .setNegativeButton("Cancel / Hủy", null)
                .show()
        }
        p.addView(row)
    }

    /** Dong chon 1 trong nhieu lua chon (hop thoai), dung chung cho moi noi can "picker". */
    private fun pickRow(p: LinearLayout, text: String, key: String, opts: List<Pair<String, String>>, default: String, onChange: (() -> Unit)? = null) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        row.addView(label(text, 14f, INK), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fun labelOf(v: String) = opts.firstOrNull { it.first == v }?.second ?: v
        val cur = prefs().getString(key, default) ?: default
        val value = label(labelOf(cur), 14f, ACCENT, true)
        row.addView(value)
        row.setOnClickListener {
            val now = prefs().getString(key, default) ?: default
            val idx = opts.indexOfFirst { it.first == now }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(text)
                .setSingleChoiceItems(opts.map { it.second }.toTypedArray(), idx) { d, which ->
                    prefs().edit().putString(key, opts[which].first).apply()
                    value.text = opts[which].second
                    d.dismiss()
                    onChange?.invoke()
                    refresh()
                }
                .setNegativeButton(L.tr("Hủy"), null)
                .show()
        }
        p.addView(row)
    }

    /** Dong chon hanh dong cho cu chi (hop thoai chon 1). */
    private fun choice(p: LinearLayout, text: String, key: String) {
        val opts = BarOverlay.ACTIONS
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        row.addView(label(text, 14f, INK), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fun labelOf(v: String) = L.tr(opts.firstOrNull { it.first == v }?.second ?: v)
        val value = label(labelOf(BarOverlay.currentAction(prefs(), key)), 14f, ACCENT, true)
        row.addView(value)
        row.setOnClickListener {
            val cur = BarOverlay.currentAction(prefs(), key)
            val idx = opts.indexOfFirst { it.first == cur }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(text)
                .setSingleChoiceItems(opts.map { L.tr(it.second) }.toTypedArray(), idx) { d, which ->
                    prefs().edit().putString(key, opts[which].first).apply()
                    value.text = L.tr(opts[which].second)
                    d.dismiss()
                }
                .setNegativeButton(L.tr("Hủy"), null)
                .show()
        }
        p.addView(row)
    }

    private fun seek(p: LinearLayout, text: String, unit: String, key: String, max: Int, def: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(label(text, 14f, INK), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val v = label("${prefs().getInt(key, def)}$unit", 14f, ACCENT, true)
        row.addView(v)
        p.addView(row, lp(16))
        val sb = SeekBar(this).apply {
            this.max = max; progress = prefs().getInt(key, def)
            progressTintList = ColorStateList.valueOf(ACCENT)
            thumbTintList = ColorStateList.valueOf(ACCENT)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, pr: Int, u: Boolean) { v.text = "$pr$unit" }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {
                    prefs().edit().putInt(key, s.progress).apply(); pushService(); refresh()
                }
            })
        }
        p.addView(sb, lp(0))
    }

    private fun btn(p: LinearLayout, text: String, primary: Boolean = false, top: Int = 12, action: () -> Unit) {
        val b = Button(this).apply {
            this.text = text; isAllCaps = false; textSize = 14f
            setTextColor(if (primary) Color.WHITE else ACCENT)
            stateListAnimator = null; minimumHeight = dp(46)
            background = RippleDrawable(
                ColorStateList.valueOf(0x33000000),
                shape(if (primary) ACCENT else TONAL, 12), shape(Color.WHITE, 12)
            )
            setOnClickListener { action(); refresh() }
        }
        p.addView(b, lp(top))
    }

    private fun field(p: LinearLayout, title: String, hint: String, value: String): EditText {
        p.addView(label(title, 12f, MUTED), lp(16))
        val e = EditText(this).apply {
            this.hint = hint; setText(value); textSize = 14f; setTextColor(INK)
            setSingleLine(true)
            background = shape(FIELD, 12, LINE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        p.addView(e, lp(6))
        return e
    }

    private fun stateRow(ok: Boolean, name: String, okText: String, badText: String): TextView {
        val color = if (ok) GOOD else BAD
        val sb = SpannableStringBuilder("●  ")
        sb.setSpan(ForegroundColorSpan(color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(name).append(":  ")
        val start = sb.length
        sb.append(if (ok) okText else badText)
        sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return TextView(this).apply { text = sb; textSize = 14f; setTextColor(INK); setPadding(0, dp(3), 0, dp(3)) }
    }

    private fun copy(name: String, value: CharSequence, toast: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(name, value))
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    private fun openBackgroundPerms() {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"),
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
        )
        for (cn in candidates) {
            try {
                val i = Intent().setComponent(cn)
                if (cn.packageName == "com.miui.securitycenter") i.putExtra("extra_pkgname", packageName)
                startActivity(i)
                return
            } catch (_: Exception) {}
        }
        openAppSettings()
        val msg = if (DeviceProfile.isXiaomi) L.tr("Vào Quyền → Quyền khác, bật \"Hiển thị cửa sổ pop-up khi chạy nền\".")
                  else L.tr("Vào phần quyền của app, bật quyền hiển thị cửa sổ pop-up và cho phép chạy nền.")
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun openAppSettings() =
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun bindPref(e: EditText, key: String) {
        e.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                prefs().edit().putString(key, s?.toString()?.trim() ?: "").apply()
            }
        })
    }

    /** Nhu bindPref nhung de trong thi xoa khoa, de noi doc dung gia tri mac dinh. */
    private fun bindField(e: EditText, key: String) {
        e.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val v = s?.toString()?.trim() ?: ""
                val ed = prefs().edit()
                if (v.isEmpty()) ed.remove(key) else ed.putString(key, v)
                ed.apply()
            }
        })
    }

    private fun openAutostart() {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
        )
        for (cn in candidates) {
            try { startActivity(Intent().setComponent(cn)); return } catch (_: Exception) {}
        }
        openAppSettings()
        Toast.makeText(this, L.tr("Tìm Tự khởi động / Autostart rồi bật Gemini Guard."), Toast.LENGTH_LONG).show()
    }

    /** Wizard lần đầu: các bước tối thiểu để app chạy được, mỗi bước có nút sửa. */
    private fun onboarding(root: LinearLayout) {
        if (prefs().getBoolean("onboard_done", false)) return
        val c = card(
            root, L.tr("Thiết lập lần đầu"),
            L.tr("Làm các bước còn dấu ✘. Bạn có thể mở lại hướng dẫn trong Công cụ và chẩn đoán.")
        )
        c.addView(stateRow(Restorer.hasPermission(this), L.tr("Quyền ghi cài đặt bảo mật"), L.tr("Đã cấp"), L.tr("Chưa cấp")))
        c.addView(stateRow(Settings.canDrawOverlays(this), L.tr("Hiển thị trên ứng dụng khác"), L.tr("Đã cấp"), L.tr("Chưa cấp")))
        c.addView(stateRow(Restorer.accessibilityOn(this), L.tr("Trợ năng (theo dõi Mi AI)"), L.tr("Đang bật"), L.tr("Đang tắt")))

        if (!Restorer.hasPermission(this))
            btn(c, L.tr("Sao chép lệnh ADB cấp quyền"), primary = true, top = 8) {
                copy("adb", "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS", L.tr("Đã sao chép lệnh ADB"))
            }
        if (!Settings.canDrawOverlays(this))
            btn(c, L.tr("Cấp quyền hiển thị trên ứng dụng khác"), top = 8) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        if (!Restorer.accessibilityOn(this))
            btn(c, L.tr("Mở cài đặt Trợ năng"), top = 8) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn(c, L.tr("Cho phép tự khởi động (chống bị dừng)"), top = 8) { openAutostart() }
        btn(c, L.tr("Đã xong, ẩn hướng dẫn"), top = 10) {
            prefs().edit().putBoolean("onboard_done", true).apply()
            recreate()
        }
    }

    /** Thử gọi trợ lý ngay (không cần giữ nút nguồn) để kiểm tra cấu hình và quyền. */
    private fun tryAssist() {
        Toast.makeText(this, L.tr("Đang gọi trợ lý…"), Toast.LENGTH_SHORT).show()
        val app = applicationContext
        Thread {
            if (Restorer.prefs(app).getBoolean("enabled", true)) Restorer.ensure(app)
            val err = Restorer.callTarget(app)
            if (err == null) GeminiSound.play(app)
            runOnUiThread {
                val msg = if (err == null) L.fmt("Đã gửi lệnh gọi %s", Restorer.targetLabel(app))
                else L.fmt("Không gọi được %s: %s", Restorer.targetLabel(app), err)
                Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
                refresh()
            }
        }.start()
    }

    /** Tim cac app co ten lien quan ngan hang / vi dien tu / VNeID trong so app da cai. */
    private fun autofillBanks() {
        val kw = listOf(
            "bank", "vietcombank", "vietinbank", "bidv", "agribank", "techcombank",
            "mbbank", "acb", "vpbank", "tpbank", "sacombank", "vneid", "vnid", "momo", "zalopay"
        )
        val found = mutableListOf<String>()
        try {
            for (app in packageManager.getInstalledApplications(0)) {
                val label = try { packageManager.getApplicationLabel(app).toString() } catch (_: Exception) { app.packageName }
                val s = (app.packageName + " " + label).lowercase()
                if (kw.any { s.contains(it) }) found += app.packageName
            }
        } catch (_: Exception) {}
        if (found.isEmpty()) {
            Toast.makeText(this, L.tr("Không tìm thấy app ngân hàng nào – hãy tự nhập tên gói"), Toast.LENGTH_LONG).show()
            return
        }
        val merged = (Filters.split(prefs().getString("bank_pkgs", "") ?: "") + found).distinct()
        prefs().edit().putString("bank_pkgs", merged.joinToString(",")).apply()
        bankPkgs.setText(merged.joinToString(","))
        Toast.makeText(this, L.fmt("Đã thêm %d tên gói", found.size), Toast.LENGTH_LONG).show()
    }

    // ---------- Màn hình chính ----------
    @Suppress("DEPRECATION")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (android.os.Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        window.statusBarColor = BG
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(22), dp(16), dp(36))
        }
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "?" }

        // Tiêu đề
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo)
        }
        head.addView(logo, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) })
        head.addView(label("Gemini Guard", 26f, INK, true))
        head.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        head.addView(TextView(this).apply {
            text = L.tr("Hỗ trợ"); textSize = 14f; setTextColor(ACCENT); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = shape(TONAL, 20)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { showSupport() }
        })
        root.addView(head, lp(0))
        root.addView(label(L.fmt("Giữ Gemini luôn là trợ lý mặc định · phiên bản %s", ver), 13f, MUTED), lp(2))

        onboarding(root)

        // Cảnh báo thiếu quyền
        warnCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(0xFFFDECEA.toInt(), 18, 0xFFF3B9B3.toInt())
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        warnCard.addView(label(L.tr("Chưa cấp quyền ghi cài đặt bảo mật"), 16f, BAD, true))
        warnCard.addView(label(
            L.tr("Thiếu quyền WRITE_SECURE_SETTINGS nên app không giữ được Google làm trợ lý ") +
                L.tr("(máy sẽ hiện bảng “Complete action using” và mở app Google). Hãy cấp bằng ADB. ") +
                L.tr("Khi cập nhật, cài đè lên bản cũ và đừng gỡ cài đặt, vì gỡ sẽ mất quyền."),
            13f, INK).apply { setPadding(0, dp(6), 0, 0) })
        if (DeviceProfile.isHonor)
            warnCard.addView(label(
                L.tr("Trên Honor/MagicOS, nếu lệnh ADB báo lỗi, hãy bật \"Tắt giám sát quyền\" (Disable permission monitoring) trong Tùy chọn nhà phát triển rồi chạy lại."),
                12f, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        btn(warnCard, L.tr("Sao chép lệnh ADB cấp quyền"), primary = true) {
            copy("adb", "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS", L.tr("Đã sao chép lệnh ADB"))
        }
        root.addView(warnCard, lp(14))

        // Trạng thái nhanh
        val st = card(root, L.tr("Trạng thái"))
        stateBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        st.addView(stateBox, lp(8))
        st.addView(label(
            L.tr("Dòng \"Trợ năng (theo dõi Mi AI)\" và \"MiCTS\" là tùy chọn: chỉ cần khi bạn muốn tự gọi Gemini lúc Mi AI hiện lên, hoặc gán cử chỉ thanh nổi cho MiCTS. Để tắt / chưa cài vẫn dùng được các chức năng chính."),
            12f, MUTED).apply { setPadding(0, dp(6), 0, 0) })

        // 1. Giữ Gemini
        val c1 = card(root, L.tr("Giữ Gemini làm trợ lý"),
            L.tr("Canh cài đặt hệ thống; nếu trợ lý bị xóa hoặc đổi sang app khác thì tự ghi lại Google."))
        sw(c1, L.tr("Canh nền và tự khôi phục Google"), null, "enabled", true)
        sw(c1, L.tr("Giữ trợ lý mặc định (assistant)"),
            L.tr("Tắt nếu bạn muốn tự quản lý mục này. Mặc định bật."),
            "keep_assistant", true)
        sw(c1, L.tr("Giữ dịch vụ giọng nói (voice_interaction_service)"),
            L.tr("Thường phải bật cùng mục trên để nút nguồn và trợ lý hoạt động đúng. Mặc định bật."),
            "keep_voice", true)
        if (DeviceProfile.isXiaomi) {
            val powerSwitch = sw(c1, L.tr("Ghi đè hành vi giữ nút nguồn"),
                L.tr("Tắt nếu bạn muốn giữ nguyên hành vi gốc của nút nguồn (menu nguồn, đèn pin…). Khi tắt, app tự trả lại giá trị cũ cho nút nguồn."),
                "power_key", true)
            btn(c1, L.tr("Trả lại menu nguồn ngay"), top = 8) {
                prefs().edit().putBoolean("power_key", false).apply()
                if (powerSwitch.isChecked) powerSwitch.isChecked = false
                else Toast.makeText(this, Restorer.restorePowerKey(this), Toast.LENGTH_LONG).show()
            }
        } else {
            c1.addView(label(
                L.tr("Máy này không phải Xiaomi/HyperOS nên app không ghi đè nút nguồn. Hãy đặt Gemini làm trợ lý mặc định trong Cài đặt của máy."),
                12f, MUTED).apply { setPadding(0, dp(10), 0, 0) })
            btn(c1, L.tr("Mở cài đặt trợ lý mặc định"), top = 8) {
                try { startActivity(Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")) }
                catch (_: Exception) {
                    try { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                    catch (_: Exception) { openAppSettings() }
                }
            }
        }
        btn(c1, L.tr("Khôi phục ngay"), primary = true) {
            val ch = Restorer.ensure(this)
            Toast.makeText(this, if (ch.isEmpty()) L.tr("Mọi thứ đã đúng") else L.fmt("Đã sửa %d mục", ch.size), Toast.LENGTH_SHORT).show()
        }

        btn(c1, L.tr("Khởi động lại Google (tắt → bật)"), top = 8) {
            Toast.makeText(this, L.tr("Đang tắt → bật lại, giữ app mở vài giây…"), Toast.LENGTH_SHORT).show()
            Thread {
                val res = Restorer.cycle(this)
                runOnUiThread { Toast.makeText(this, res, Toast.LENGTH_LONG).show(); refresh() }
            }.start()
        }
        sw(c1, L.tr("Tự tắt → bật lại Google sau khi khởi động máy"),
            L.tr("Để hệ thống nhận lại Google sau mỗi lần khởi động. Cần bật Tự khởi động cho app."),
            "cycle_boot", true)
        seek(c1, L.tr("Đợi sau khi máy khởi động xong"), L.tr(" giây"), "cycle_boot_delay_s", 30, 0)
        btn(c1, L.tr("Cho phép tự khởi động (chống bị dừng)"), top = 8) { openAutostart() }

        // 2. Thanh điều hướng AI
        val c2 = card(root, L.tr("Thanh điều hướng AI"),
            L.tr("Thanh nhỏ nổi ở đáy màn hình. Chọn việc cho từng cử chỉ bên dưới."))
        sw(c2, L.tr("Bật thanh điều hướng AI"), null, "bar", false)
        btn(c2, L.tr("Cấp quyền hiển thị trên ứng dụng khác")) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        ctsPkg = field(c2, L.tr("Tên gói MiCTS"), "com.parallelc.micts", prefs().getString("cts_pkg", "com.parallelc.micts") ?: "")
        bindField(ctsPkg, "cts_pkg")
        choice(c2, L.tr("Chạm đúp"), "bar_dt")
        choice(c2, L.tr("Giữ"), "bar_lp")
        trPkg = field(c2, L.tr("Dịch màn hình: tên gói app dịch"), BarOverlay.defaultTrPkg().ifEmpty { L.tr("Để trống nếu không dùng") },
            prefs().getString("tr_pkg", BarOverlay.defaultTrPkg()) ?: "")
        bindPref(trPkg, "tr_pkg")
        trTxt = field(c2, L.tr("Dịch màn hình: chữ trên thẻ (phân cách bằng dấu phẩy)"), BarOverlay.TR_TEXT, prefs().getString("tr_text", BarOverlay.TR_TEXT) ?: "")
        bindPref(trTxt, "tr_text")
        seek(c2, L.tr("Chiều rộng"), "%", "bar_w", 100, 40)
        seek(c2, L.tr("Chiều cao"), " dp", "bar_h", 60, 20)
        seek(c2, L.tr("Cách đáy (kéo thanh để đổi)"), " dp", "bar_y", BarOverlay.MAX_Y_DP, BarOverlay.MIN_Y_DP)
        seek(c2, L.tr("Độ đậm (0 = vô hình)"), "%", "bar_a", 100, 30)

        // 3. Mi AI
        val c3 = card(root, L.tr("Gọi Gemini khi Mi AI bật lên") + L.tr(" (không dùng được ngân hàng và VNeID, muốn dùng phải tắt đi)"),
            L.tr("Khi phát hiện Mi AI (giữ nút nguồn, gọi giọng nói…), app đóng nó (bấm Back và chạm vào chỗ trống) rồi gọi Gemini lên thay thế. Cần bật Trợ năng cho Gemini Guard – nhiều app ngân hàng và VNeID sẽ chặn hoặc cảnh báo khi Trợ năng đang bật, nên tắt mục này (và tắt Trợ năng) trước khi dùng các app đó."))
        if (DeviceProfile.isHonor)
            c3.addView(label(L.tr("Trên Honor/MagicOS: \"Mi AI\" ở đây là YOYO; mặc định đã điền sẵn tên gói YOYO."),
                12f, MUTED).apply { setPadding(0, dp(2), 0, dp(4)) })
        sw(c3, L.tr("Tự gọi Gemini khi Mi AI bật lên"), null, "mi_on", false)
        pickRow(c3, L.tr("Gọi đến"), "target_pkg",
            listOf(
                Restorer.GOOGLE_PKG to "Gemini (Google)",
                Restorer.CHATGPT_PKG to "ChatGPT",
                Restorer.TARGET_CUSTOM to L.tr("Khác (nhập tên gói)")
            ), Restorer.GOOGLE_PKG)
        customTarget = field(c3, L.tr("Tên gói trợ lý khác (dùng khi chọn Khác)"), "com.example.assistant",
            prefs().getString("target_custom", "") ?: "")
        bindField(customTarget, "target_custom")
        sw(c3, L.tr("Đặt ChatGPT làm trợ lý hệ thống"),
            L.tr("Tắt (mặc định): ChatGPT chỉ được gọi khi Mi AI bật, trợ lý hệ thống vẫn là Gemini. Bật: sau khi bật phải bấm \"Tắt → bật lại\" ở thẻ đầu để hệ thống đổi trợ lý."),
            "sys_chatgpt", false)
        val chatgptNote = if (DeviceProfile.isXiaomi)
            L.tr("Khi Mi AI bật lên, nếu chọn ChatGPT thì app mở lớp phủ ChatGPT thay cho Gemini (qua màn hình trợ lý ChatGPT cho phép gọi), còn Gemini vẫn là trợ lý hệ thống nên giữ nút nguồn vẫn mở Gemini. Muốn cả nút nguồn mở ChatGPT thì bật công tắc bên dưới.")
        else
            L.tr("Khi Mi AI bật lên, nếu chọn ChatGPT thì app mở lớp phủ ChatGPT thay cho Gemini. Trên máy này app không đổi được hành vi nút nguồn.")
        c3.addView(label(chatgptNote, 12f, MUTED).apply { setPadding(0, dp(2), 0, dp(6)) })
        btn(c3, L.tr("Thử mở lớp phủ ChatGPT sau 5 giây (bấm Home ngay)"), top = 8) {
            Toast.makeText(this, L.tr("Hãy bấm Home ngay, sau 5 giây app sẽ thử mở ChatGPT từ nền"), Toast.LENGTH_LONG).show()
            val app = applicationContext
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ Restorer.callChatGpt(app) }, 5000)
        }
        btn(c3, L.tr("Chẩn đoán ChatGPT (sao chép)"), top = 8) {
            val txt = Restorer.chatGptInfo(this)
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("chatgpt", txt))
            Toast.makeText(this, L.tr("Đã sao chép chẩn đoán ChatGPT, dán vào chat gửi cho Claude"), Toast.LENGTH_LONG).show()
        }
        btn(c3, L.tr("Cấp quyền cửa sổ pop-up khi chạy nền"), top = 8) { openBackgroundPerms() }
        c3.addView(label(
            if (DeviceProfile.isXiaomi)
                L.tr("HyperOS thường chặn app mở cửa sổ khi chạy nền mà không báo lỗi. Nếu ChatGPT không hiện khi Mi AI bật, hãy bật quyền \"Hiển thị cửa sổ pop-up khi chạy nền\" ở nút trên.")
            else
                L.tr("MagicOS/Android thường chặn app mở cửa sổ khi chạy nền. Nếu trợ lý không hiện, hãy bật quyền hiển thị cửa sổ pop-up và cho phép app chạy nền ở nút trên."),
            12f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
        seek(c3, L.tr("Vị trí chạm – ngang"), "%", "mi_tx", 100, 8)
        seek(c3, L.tr("Vị trí chạm – dọc"), "%", "mi_ty", 100, 10)
        seek(c3, L.tr("Chờ trước khi chạm đóng Mi AI (0 = nhanh nhất)"), " ms", "mi_tapwait", 400, 0)
        seek(c3, L.tr("Độ trễ trước khi gọi Gemini"), " ms", "mi_delay", 1500, 350)
        miPkgs = field(c3, L.tr("Tên gói trợ lý cần theo dõi (Mi AI / YOYO)"),
            if (DeviceProfile.isHonor) "com.hihonor.assistant, com.hihonor.magicvoice" else "com.miui.voiceassist",
            prefs().getString("mi_pkgs", MiAiWatcher.defaultPkgs()) ?: "")
        bindField(miPkgs, "mi_pkgs")
        miInc = field(c3, L.tr("Chỉ gọi Gemini khi cửa sổ Mi AI là (để trống = mọi cửa sổ)"), L.tr("Để trống = mọi cửa sổ"), prefs().getString("mi_inc", MiAiWatcher.defaultInc()) ?: "")
        bindPref(miInc, "mi_inc")
        miExc = field(c3, L.tr("Bỏ qua khi cửa sổ Mi AI là (mở app, dịch màn hình…)"), L.tr("Để trống = không bỏ qua cửa sổ nào"), prefs().getString("mi_exc", MiAiWatcher.defaultExc()) ?: "")
        bindPref(miExc, "mi_exc")
        btn(c3, L.tr("Đặt lại bộ lọc về mặc định"), top = 12) {
            prefs().edit().remove("mi_inc").remove("mi_exc").apply()
            refresh()
        }
        btn(c3, L.tr("Mở cài đặt Trợ năng"), primary = true) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn(c3, L.tr("Thử gọi trợ lý ngay (không cần giữ nút nguồn)"), top = 8) { tryAssist() }
        sw(c3, L.tr("Tự tắt Trợ năng khi mở app ngân hàng / VNeID"),
            L.tr("Cần quyền \"Truy cập sử dụng\". Khi mở app trong danh sách, app tắt Trợ năng; khi rời đi sẽ bật lại nếu \"Tự gọi Gemini\" đang bật."),
            "bank_guard", false)
        bankPkgs = field(c3, L.tr("Tên gói app ngân hàng / VNeID (cách nhau bằng dấu phẩy)"),
            L.tr("Bấm nút bên dưới để tự tìm, hoặc tự nhập"), prefs().getString("bank_pkgs", "") ?: "")
        bindField(bankPkgs, "bank_pkgs")
        btn(c3, L.tr("Tự tìm app ngân hàng đã cài"), top = 8) { autofillBanks() }
        btn(c3, L.tr("Cấp quyền Truy cập sử dụng"), top = 8) {
            try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            catch (_: Exception) { openAppSettings() }
        }
        btn(c3, L.tr("Cho phép cài đặt bị hạn chế (Android 13+)"), top = 8) { openAppSettings() }

        // 4. Âm thanh
        val c4 = card(root, L.tr("Hiệu ứng âm thanh và rung"),
            L.tr("Phát âm thanh / rung nhẹ khi Gemini hiện lên: giữ nút nguồn, gọi giọng nói (qua Mi AI) hoặc dùng thanh. Cần bật Trợ năng để nhận ra khi giữ nút nguồn."))
        sw(c4, L.tr("Phát âm thanh khi Gemini hiện lên"), null, "snd_on", false)
        seek(c4, L.tr("Âm lượng hiệu ứng"), "%", "snd_vol", 100, 100)
        seek(c4, L.tr("Khuếch đại thêm (0 = tắt)"), " dB", "snd_gain", 15, 0)
        seek(c4, L.tr("Bỏ qua đầu âm thanh để bớt trễ"), " ms", "snd_skip", 600, 0)
        seek(c4, L.tr("Chống lặp âm thanh"), L.tr(" giây"), "snd_gap_s", 10, 3)
        sndCls = field(c4, L.tr("Lọc tên cửa sổ Google (tùy chọn)"), L.tr("Để trống = mọi cửa sổ"), prefs().getString("snd_cls", "") ?: "")
        bindField(sndCls, "snd_cls")
        btn(c4, L.tr("Thử âm thanh"), primary = true) { GeminiSound.play(this, force = true) }
        btn(c4, L.tr("Chọn âm thanh riêng…"), top = 8) {
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, REQ_SOUND)
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không mở được trình chọn tệp: ") + e.message, Toast.LENGTH_LONG).show()
            }
        }
        if (!prefs().getString("snd_file", "").isNullOrBlank())
            btn(c4, L.tr("Dùng âm thanh mặc định"), top = 8) {
                val old = prefs().getString("snd_file", "") ?: ""
                prefs().edit().remove("snd_file").apply()
                try { java.io.File(old).delete() } catch (_: Exception) {}
                GeminiSound.release(); GeminiSound.warm(this)
                Toast.makeText(this, L.tr("Đã dùng lại âm thanh mặc định"), Toast.LENGTH_SHORT).show()
                recreate()
            }
        sw(c4, L.tr("Rung nhẹ khi Gemini hiện lên"), L.tr("Máy rung ngắn tạo cảm giác cửa sổ bật ra. Dùng chung bộ lọc cửa sổ và chống lặp với âm thanh ở trên."), "vib_on", false)
        seek(c4, L.tr("Độ mạnh rung"), "%", "vib_str", 100, 60)
        seek(c4, L.tr("Thời gian rung"), " ms", "vib_ms", 80, 25)
        btn(c4, L.tr("Thử rung"), top = 8) { Haptic.pop(this, force = true) }

        // 5. Công cụ
        val c5 = card(root, L.tr("Công cụ và chẩn đoán"))
        c5.addView(label(L.tr("Phím tắt thủ công: mở Control Center, bấm Chỉnh sửa (biểu tượng bút chì) rồi kéo ô \"Trợ năng Gemini Guard\" vào (nằm trong mục Phím nhanh ứng dụng, có thể phải cuộn xuống). Nếu không thấy ô, nhấn giữ icon app Gemini Guard và chọn \"Bật/tắt Trợ năng\", có thể kéo mục đó ra màn hình chính. Bấm để tắt Trợ năng NGAY TRƯỚC khi mở app ngân hàng / VNeID, chắc hơn tự động vì không phải chờ hỏi định kỳ. Cần đã cấp quyền WRITE_SECURE_SETTINGS."), 12f, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        if (prefs().getBoolean("acc_tile_off", false))
            c5.addView(label(L.tr("Trợ năng đang tắt bằng phím tắt Control Center, hãy bật lại ở phím tắt đó khi cần dùng Mi AI"), 13f, ACCENT).apply { setPadding(0, dp(4), 0, 0) })
        langRow(c5)
        btn(c5, L.tr("Cài đặt pin / tự khởi động của app"), top = 14) { openAppSettings() }
        btn(c5, L.tr("Sao chép cài đặt liên quan Mi AI / nút nguồn"), top = 8) {
            copy("settings", Restorer.dumpRelated(this), L.tr("Đã sao chép – dán vào chat để gửi cho Claude"))
        }
        btn(c5, L.tr("Sao chép trạng thái"), top = 8) { copy("status", status.text, L.tr("Đã sao chép")) }
        btn(c5, L.tr("Chia sẻ chẩn đoán (gửi hỗ trợ)"), top = 8) {
            Diagnostics.share(this, Diagnostics.build(this))
        }
        btn(c5, L.tr("Mở lại hướng dẫn thiết lập"), top = 8) {
            prefs().edit().putBoolean("onboard_done", false).apply()
            recreate()
        }
        btn(c5, L.tr("Xuất cấu hình (JSON)"), top = 8) {
            try {
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "gemini-guard-backup.json")
                }, REQ_EXPORT)
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không mở được trình lưu tệp: ") + e.message, Toast.LENGTH_LONG).show()
            }
        }
        btn(c5, L.tr("Nhập cấu hình (JSON)"), top = 8) {
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "application/json"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, REQ_IMPORT)
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không mở được trình chọn tệp: ") + e.message, Toast.LENGTH_LONG).show()
            }
        }
        val apiOn = !prefs().getString("api_token", "").isNullOrBlank()
        btn(c5, if (apiOn) L.tr("Đổi mã API điều khiển ngoài") else L.tr("Bật API điều khiển ngoài (Tasker)"), top = 8) {
            val t = CommandReceiver.newToken()
            prefs().edit().putString("api_token", t).apply()
            copy("api_token", t, L.tr("Đã tạo mã API và sao chép – gửi kèm extra token"))
            recreate()
        }
        if (apiOn) btn(c5, L.tr("Tắt API điều khiển ngoài"), top = 8) {
            prefs().edit().remove("api_token").apply()
            Toast.makeText(this, L.tr("Đã tắt API điều khiển ngoài"), Toast.LENGTH_SHORT).show()
            recreate()
        }
        c5.addView(label(
            L.tr("API ngoài dùng để Tasker/MacroDroid gọi trợ lý hoặc bật/tắt thanh. Cần gửi extra token. Hành động: ") +
                "com.gemini.guard.ACTION_ASSIST, ACTION_BAR_ON, ACTION_BAR_OFF, ACTION_TOGGLE_ACC.",
            12f, MUTED).apply { setPadding(0, dp(6), 0, 0) })
        status = TextView(this).apply {
            textSize = 12f; setTextColor(MUTED); typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            background = shape(FIELD, 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        c5.addView(status, lp(14))

        root.addView(label("Gemini Guard v$ver · by Đỗ Đạt - Yume. System", 12f, MUTED).apply { gravity = android.view.Gravity.CENTER },
            lp(22))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        pushService()
    }

    override fun onResume() {
        super.onResume()
        // Goi pushService truoc de HyperOS kip ap dung cai dat, roi moi doc trang thai hien thi.
        pushService()
        refresh()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        when (requestCode) {
            REQ_EXPORT -> try {
                contentResolver.openOutputStream(uri)?.use { it.write(Backup.exportJson(this).toByteArray()) }
                Toast.makeText(this, L.tr("Đã xuất cấu hình"), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không xuất được: ") + e.message, Toast.LENGTH_LONG).show()
            }
            REQ_IMPORT -> try {
                val txt = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException(L.tr("không đọc được tệp"))
                val n = Backup.importJson(this, txt)
                Toast.makeText(this, L.fmt("Đã nhập %d mục cấu hình", n), Toast.LENGTH_LONG).show()
                recreate()
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không nhập được: ") + e.message, Toast.LENGTH_LONG).show()
            }
            REQ_SOUND -> try {
                val dst = java.io.File(filesDir, "gemini_sound_custom")
                contentResolver.openInputStream(uri)?.use { input ->
                    dst.outputStream().use { out -> input.copyTo(out) }
                } ?: throw IllegalStateException(L.tr("không đọc được tệp"))
                prefs().edit().putString("snd_file", dst.absolutePath).apply()
                GeminiSound.release(); GeminiSound.warm(this)
                Toast.makeText(this, L.tr("Đã đặt âm thanh riêng"), Toast.LENGTH_LONG).show()
                recreate()
            } catch (e: Exception) {
                Toast.makeText(this, L.tr("Không đọc được âm thanh: ") + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refresh() {
        for ((e, k) in listOf(miInc to "mi_inc", miExc to "mi_exc")) {
            val v = prefs().getString(k, if (k == "mi_inc") MiAiWatcher.defaultInc() else MiAiWatcher.defaultExc()) ?: ""
            if (e.text.toString().trim() != v) e.setText(v)
        }
        val hasPerm = Restorer.hasPermission(this)
        warnCard.visibility = if (hasPerm) View.GONE else View.VISIBLE

        val r = contentResolver
        val wantAssist = Restorer.assistComponent(this)
        val googleOk = Settings.Secure.getString(r, "assistant") == wantAssist &&
            Settings.Secure.getString(r, "voice_interaction_service") == wantAssist
        val pkg = prefs().getString("cts_pkg", "com.parallelc.micts") ?: ""
        val cts = packageManager.getLaunchIntentForPackage(pkg) != null

        stateBox.removeAllViews()
        stateBox.addView(stateRow(hasPerm, L.tr("Quyền ghi cài đặt bảo mật"), L.tr("Đã cấp"), L.tr("Chưa cấp")))
        stateBox.addView(stateRow(googleOk, L.tr("Trợ lý mặc định"), Restorer.effectiveLabel(this), L.tr("Chưa đúng")))
        stateBox.addView(stateRow(Settings.canDrawOverlays(this), L.tr("Hiển thị trên ứng dụng khác"), L.tr("Đã cấp"), L.tr("Chưa cấp")))
        stateBox.addView(stateRow(Restorer.accessibilityOn(this), L.tr("Trợ năng (theo dõi Mi AI)"), L.tr("Đang bật"), L.tr("Đang tắt")))
        stateBox.addView(stateRow(cts, "MiCTS", L.tr("Đã cài"), L.tr("Chưa cài")))

        status.text = Restorer.status(this) +
            L.tr("\n\nCửa sổ trợ lý vừa thấy (mới nhất trước):\n") +
            (prefs().getString("gseen", "")?.ifBlank { L.tr("(chưa có)") } ?: "") +
            L.tr("\n\nỨng dụng vừa thấy (để tìm tên gói Mi AI):\n") +
            (prefs().getString("seen", "")?.ifBlank { L.tr("(chưa có – bật Trợ năng rồi gọi Mi AI)") } ?: "")
    }
}
