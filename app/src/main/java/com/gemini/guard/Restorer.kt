package com.gemini.guard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Restorer {
    const val GOOGLE_ASSIST =
        "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"
    const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"
    const val CHATGPT_PKG = "com.openai.chatgpt"
    const val CHATGPT_ASSIST_ACTIVITY = "com.openai.voice.assistant.AssistantActivity"
    private val SECURE_KEYS = listOf("assistant", "voice_interaction_service")

    /**
     * App duoc GOI khi phat hien Mi AI (giong noi) hoac bam "Gọi Gemini" tren thanh dieu huong.
     * Chi Google/Gemini la tich hop that o muc he thong (VoiceInteractionService) nen giu nut
     * nguon luon mo duoc Gemini truc tiep. ChatGPT (va app khac) khong co kieu tich hop nay,
     * nen chi mo duoc bang cach mo thang app do (nhu mo mot app binh thuong), khong the thay
     * the duoc hanh vi giu nut nguon o muc he thong.
     */
    fun targetPkg(c: Context): String = prefs(c).getString("target_pkg", GOOGLE_PKG) ?: GOOGLE_PKG

    fun targetLabel(c: Context): String = when (targetPkg(c)) {
        GOOGLE_PKG -> "Google"
        CHATGPT_PKG -> "ChatGPT"
        else -> targetPkg(c)
    }

    /**
     * Thanh phan VoiceInteractionService cua tro ly muc tieu (ghi vao assistant / voice_interaction_service).
     * Google: co dinh. ChatGPT: tim luc chay trong ChatGPT da cai; khong tim thay thi dung Google.
     */
    fun assistComponent(c: Context): String {
        if (targetPkg(c) != CHATGPT_PKG || !prefs(c).getBoolean("sys_chatgpt", false)) return GOOGLE_ASSIST
        val found = try {
            c.packageManager.queryIntentServices(Intent("android.service.voice.VoiceInteractionService").setPackage(CHATGPT_PKG), 0)
                .firstOrNull()?.serviceInfo?.let { ComponentName(it.packageName, it.name).flattenToString() }
        } catch (_: Exception) { null }
        return found ?: GOOGLE_ASSIST
    }

    /** Muc tieu thuc te dang duoc dung (ChatGPT chi tinh khi tim thay dich vu tro ly cua no). */
    fun effectiveLabel(c: Context): String = if (assistComponent(c) == GOOGLE_ASSIST) "Google" else targetLabel(c)

    /** Thuc hien goi tro ly muc tieu. Google: dung API an hien dung lop phu tro ly that (nhu cu).
     * Khac Google (vd ChatGPT): chi mo thang app do len, khong phai lop phu tro ly he thong that.
     * Tra ve null = thanh cong, khac null = mo ta loi. */
    fun callTarget(c: Context): String? {
        if (targetPkg(c) == GOOGLE_PKG) return callGoogle(c)
        if (targetPkg(c) == CHATGPT_PKG) return callChatGpt(c)
        val pkg = targetPkg(c)
        val i = c.packageManager.getLaunchIntentForPackage(pkg)
            ?: return L.fmt("Chưa cài %s (%s)", targetLabel(c), pkg)
        return try { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); null }
        catch (e: Exception) { e.message ?: "error" }
    }

    /**
     * Goi Gemini theo 3 buoc: phien tro ly that (hidden API) -> ACTION_ASSIST de he thong phan giai
     * -> mo thang app Gemini. Ghi lai loi tung buoc de chan doan khi Android moi doi API an.
     */
    private fun callGoogle(c: Context): String? {
        val hiddenErr = GeminiCall.show()
        if (hiddenErr == null) {
            noteAssist(c, "phiên trợ lý hệ thống: OK")
            return null
        }

        // Chi gui ACTION_ASSIST khi he thong thuc su co activity xu ly, tranh "gui xong nhung khong hien gi".
        val assist = Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val assistErr: String? = if (assist.resolveActivity(c.packageManager) == null) {
            "không có activity xử lý ACTION_ASSIST"
        } else try {
            c.startActivity(assist)
            null
        } catch (e: Exception) { e.message ?: e.javaClass.simpleName }
        if (assistErr == null) {
            noteAssist(c, "phiên trợ lý lỗi ($hiddenErr) → ACTION_ASSIST: đã gửi")
            return null
        }

        val gem = c.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
        return try {
            if (gem != null) {
                c.startActivity(gem.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                noteAssist(c, "phiên trợ lý lỗi ($hiddenErr) → mở app Gemini")
                null
            } else {
                val msg = L.fmt("Không gọi được Gemini: %s (ACTION_ASSIST: %s)", hiddenErr, assistErr)
                noteAssist(c, msg)
                msg
            }
        } catch (e: Exception) {
            val msg = L.fmt("Không gọi được Gemini: %s (ACTION_ASSIST: %s, app Gemini: %s)",
                hiddenErr, assistErr, e.message ?: e.javaClass.simpleName)
            noteAssist(c, msg)
            msg
        }
    }

    /** Ghi ket qua lan goi Gemini gan nhat de chan doan tren trang thai (giong chatgpt_last). */
    private fun noteAssist(c: Context, msg: String) {
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        prefs(c).edit().putString("assist_last", "$t  $msg").apply()
    }

    /**
     * Mo man hinh tro ly cua ChatGPT. Thu lan luot: Activity tro ly (da tim duoc qua adb), roi
     * ACTION_ASSIST gioi han vao ChatGPT, roi mo app thuong. Ghi ket qua tung buoc vao chatgpt_last.
     * Luu y: startActivity khong nem loi khi HyperOS chan "cua so pop-up khi chay nen", nen
     * "da gui lenh mo" chua chac la da hien.
     */
    fun callChatGpt(c: Context): String? {
        val pm = c.packageManager
        val launch = pm.getLaunchIntentForPackage(CHATGPT_PKG)
            ?: return L.fmt("Chưa cài %s (%s)", "ChatGPT", CHATGPT_PKG)
        val attempts = listOf(
            "ACTION_ASSIST" to Intent(Intent.ACTION_ASSIST).setPackage(CHATGPT_PKG),
            "launcher" to launch
        )
        val steps = StringBuilder()
        var result: String? = "không mở được"

        // Neu ChatGPT dang la tro ly he thong: goi lop phu that qua phien tro ly (giong Gemini)
        val cur = Settings.Secure.getString(c.contentResolver, "voice_interaction_service")
        if (cur != null && cur.startsWith("$CHATGPT_PKG/")) {
            val err = GeminiCall.showChecked()
            if (err == null) {
                prefs(c).edit().putString("chatgpt_last",
                    SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date()) + "  phiên trợ lý hệ thống: đã gọi").apply()
                return null
            }
            steps.append("phiên trợ lý: ").append(err).append("; ")
        }
        for ((name, base) in attempts) {
            try {
                c.startActivity(Intent(base).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                steps.append(name).append(": đã gửi lệnh mở")
                result = null
                break
            } catch (e: Exception) {
                steps.append(name).append(": ").append(e.javaClass.simpleName).append("; ")
            }
        }
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        prefs(c).edit().putString("chatgpt_last", "$t  $steps").apply()
        return result
    }

    /** Chan doan ChatGPT: da cai chua, AssistantActivity co export khong, co xu ly ACTION_ASSIST / la VoiceInteractionService khong. */
    fun chatGptInfo(c: Context): String {
        val pm = c.packageManager
        val pi = try { pm.getPackageInfo(CHATGPT_PKG, 0) } catch (_: Exception) { null }
            ?: return "ChatGPT ($CHATGPT_PKG): chưa cài."
        val sb = StringBuilder("ChatGPT ").append(pi.versionName).append("\n")
        val ai = try { pm.getActivityInfo(ComponentName(CHATGPT_PKG, CHATGPT_ASSIST_ACTIVITY), 0) } catch (_: Exception) { null }
        sb.append("AssistantActivity: ")
            .append(if (ai == null) "không tìm thấy" else "exported=${ai.exported}, enabled=${ai.enabled}").append("\n")
        val assist = try { pm.queryIntentActivities(Intent(Intent.ACTION_ASSIST).setPackage(CHATGPT_PKG), 0) } catch (_: Exception) { emptyList<android.content.pm.ResolveInfo>() }
        sb.append("Activity xử lý ACTION_ASSIST: ")
            .append(if (assist.isEmpty()) "không có" else assist.joinToString { it.activityInfo.name }).append("\n")
        val vis = try { pm.queryIntentServices(Intent("android.service.voice.VoiceInteractionService").setPackage(CHATGPT_PKG), 0) } catch (_: Exception) { emptyList<android.content.pm.ResolveInfo>() }
        sb.append("VoiceInteractionService: ")
            .append(if (vis.isEmpty()) "không có" else vis.joinToString { it.serviceInfo.name }).append("\n")
        sb.append("Trợ lý hệ thống hiện tại: ").append(Settings.Secure.getString(c.contentResolver, "assistant") ?: "null").append("\n")
        val last = prefs(c).getString("chatgpt_last", "") ?: ""
        if (last.isNotBlank()) sb.append("Lần mở gần nhất: ").append(last).append("\n")
        return sb.toString()
    }

    private const val POWER_KEY = "power_button_long_press"
    private const val POWER_VALUE = 5          // 5 = mo tro ly (assistant)
    private const val POWER_ORIG = "power_orig"

    /** Giá trị gốc của nút nguồn trước khi app ghi đè; -1 = chưa lưu. */
    fun powerOriginal(c: Context): Int = prefs(c).getInt(POWER_ORIG, -1)

    /** Lưu giá trị gốc một lần trước khi ghi đè 5 (bỏ qua nếu đã lưu hoặc đang là 5). */
    private fun rememberPower(c: Context, cur: Int) {
        if (cur < 0 || cur == POWER_VALUE) return
        if (prefs(c).contains(POWER_ORIG)) return
        prefs(c).edit().putInt(POWER_ORIG, cur).apply()
    }

    /**
     * Trả lại hành vi gốc cho nút nguồn (thường là 1 = menu nguồn). Gọi khi người dùng tắt
     * "Ghi đè hành vi giữ nút nguồn". Nếu chưa từng lưu giá trị gốc (bản cũ đã ghi 5) thì dùng 1.
     */
    fun restorePowerKey(c: Context): String {
        if (!DeviceProfile.isXiaomi) return L.tr("Chỉ áp dụng cho Xiaomi/HyperOS.")
        if (!hasPermission(c)) return L.tr("Chưa cấp quyền WRITE_SECURE_SETTINGS. Hãy cấp bằng ADB trước.")
        val r = c.contentResolver
        val cur = Settings.Global.getInt(r, POWER_KEY, -1)
        val orig = powerOriginal(c).let { if (it >= 0) it else 1 }
        return try {
            if (cur != orig) Settings.Global.putInt(r, POWER_KEY, orig)
            prefs(c).edit().remove(POWER_ORIG).apply()
            log(c, L.fmt("Trả lại nút nguồn: [%d] -> %d", cur, orig))
            L.fmt("Đã trả lại hành vi gốc của nút nguồn (giá trị %d). Giữ nút nguồn để thử menu nguồn.", orig)
        } catch (e: SecurityException) {
            L.tr("Lỗi quyền: ") + e.message
        }
    }

    const val ACC_KEY = "enabled_accessibility_services"

    private fun enabledList(r: android.content.ContentResolver): List<String> =
        (Settings.Secure.getString(r, ACC_KEY) ?: "").split(":").filter { it.isNotBlank() }

    fun accessibilityOn(c: Context): Boolean {
        val cn = ComponentName(c, MiAiWatcher::class.java)
        return enabledList(c.contentResolver).any { ComponentName.unflattenFromString(it) == cn } &&
            Settings.Secure.getInt(c.contentResolver, "accessibility_enabled", 0) == 1
    }

    fun prefs(c: Context) = c.getSharedPreferences("guard", Context.MODE_PRIVATE)

    /** Nguoi dung co muon app ghi de hanh vi giu nut nguon khong (mac dinh: co).
     *  Chi Xiaomi/HyperOS moi co khoa power_button_long_press; may khac tra ve false. */
    fun enforcePowerKey(c: Context) = DeviceProfile.isXiaomi && prefs(c).getBoolean("power_key", true)

    fun hasPermission(c: Context) =
        c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    /** Ghi lại giá trị đúng nếu bị đổi. Trả về mô tả những gì đã sửa. */
    fun ensure(c: Context): List<String> {
        val changes = mutableListOf<String>()
        if (!hasPermission(c)) return changes
        if (cycling) return changes   // dang tat -> bat lai Google, dung can thiep
        val r = c.contentResolver
        try {
            val want = assistComponent(c)
            for (k in SECURE_KEYS) {
                val cur = Settings.Secure.getString(r, k)
                if (cur != want) {
                    Settings.Secure.putString(r, k, want)
                    changes += "$k: [${cur ?: "null"}] -> ${effectiveLabel(c)}"
                }
            }
            if (enforcePowerKey(c)) {
                val p = Settings.Global.getInt(r, POWER_KEY, -1)
                if (p != POWER_VALUE) {
                    rememberPower(c, p)
                    Settings.Global.putInt(r, POWER_KEY, POWER_VALUE)
                    changes += "$POWER_KEY: [$p] -> $POWER_VALUE"
                }
            }
            // Khi nguoi dung tat Tro nang bang phim tat (acc_tile_off), khong tu bat lai o day
            if (prefs(c).getBoolean("mi_on", false) && !accessibilityOn(c) && !prefs(c).getBoolean("acc_tile_off", false)) {
                val cn = ComponentName(c, MiAiWatcher::class.java)
                val list = enabledList(r).filter { ComponentName.unflattenFromString(it) != cn }
                Settings.Secure.putString(r, ACC_KEY, (list + cn.flattenToString()).joinToString(":"))
                Settings.Secure.putInt(r, "accessibility_enabled", 1)
                changes += L.tr("Trợ năng MiAiWatcher: TẮT -> BẬT")
            }
        } catch (e: SecurityException) {
            changes += L.tr("Lỗi quyền: ") + e.message
        }
        if (changes.isNotEmpty()) log(c, changes.joinToString("; "))
        return changes
    }

    // ---------- Tat -> bat lai Google (giong app Gemini Restore) ----------
    @Volatile var cycling = false
    private val cycleBusy = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Ngat tro ly (de trong), cho, bat lai tro ly muc tieu va dat nut nguon = 5 (neu nguoi dung
     * con bat "ghi de nut nguon"), roi doc lai de kiem tra.
     * Co Thread.sleep nen PHAI chay tren luong nen. Tra ve mo ta ket qua.
     */
    fun cycle(c: Context): String {
        if (!hasPermission(c)) return L.tr("Chưa cấp quyền WRITE_SECURE_SETTINGS. Hãy cấp bằng ADB trước.")
        if (!cycleBusy.compareAndSet(false, true)) return L.tr("Đang chạy, chờ vài giây rồi thử lại.")
        cycling = true
        val r = c.contentResolver
        val oldAssistant = Settings.Secure.getString(r, "assistant")
        val oldService = Settings.Secure.getString(r, "voice_interaction_service")
        val oldPower = Settings.Global.getString(r, POWER_KEY)
        val msg = try {
            runCycle(c)
        } catch (e: Throwable) {
            try {
                Settings.Secure.putString(r, "voice_interaction_service", oldService)
                Settings.Secure.putString(r, "assistant", oldAssistant)
                if (enforcePowerKey(c)) Settings.Global.putString(r, POWER_KEY, oldPower)
            } catch (_: Throwable) {}
            L.fmt("Không hoàn tất: %s: %s. Đã trả về giá trị cũ.", e.javaClass.simpleName, e.message)
        } finally {
            cycling = false
            cycleBusy.set(false)
        }
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        prefs(c).edit().putString("cycle_last", "$t  $msg").apply()
        log(c, L.tr("tắt→bật Google: ") + msg)
        return msg
    }

    private fun runCycle(c: Context): String {
        val r = c.contentResolver
        val want = assistComponent(c)
        val label = effectiveLabel(c)
        val cn = ComponentName.unflattenFromString(want)!!
        val info = try { c.packageManager.getServiceInfo(cn, 0) } catch (_: Exception) { null }
        if (info == null || !info.enabled) return L.fmt("Không tìm thấy dịch vụ trợ lý %s đang bật; chưa thay đổi gì.", label)

        val offMs = prefs(c).getInt("cycle_off_ms", 1500).toLong()
        val setPower = enforcePowerKey(c)
        Settings.Secure.putString(r, "voice_interaction_service", "")
        Settings.Secure.putString(r, "assistant", "")
        Thread.sleep(offMs)

        Settings.Secure.putString(r, "voice_interaction_service", want)
        Settings.Secure.putString(r, "assistant", want)
        if (setPower) {
            rememberPower(c, Settings.Global.getInt(r, POWER_KEY, -1))
            Settings.Global.putString(r, POWER_KEY, POWER_VALUE.toString())
        }
        Thread.sleep(1000)

        val ok = Settings.Secure.getString(r, "assistant") == want &&
            Settings.Secure.getString(r, "voice_interaction_service") == want &&
            (!setPower || Settings.Global.getString(r, POWER_KEY) == POWER_VALUE.toString())
        return if (ok) L.fmt("Đã tắt → bật lại %s; đọc lại đúng các giá trị. Hãy thử giữ nút nguồn.", label)
        else L.tr("Đã chạy nhưng HyperOS chưa giữ đúng giá trị (hệ thống có thể đã tự đổi lại). Thử bấm lại.")
    }

    /** Goi tu BootReceiver (luong nen). Moi lan khoi dong may chi chay 1 lan. */
    fun cycleOnBoot(c: Context) {
        val pr = prefs(c)
        val bc = Settings.Global.getInt(c.contentResolver, "boot_count", -1)
        if (bc != -1 && bc == pr.getInt("last_cycle_boot", -2)) return
        pr.edit().putInt("last_cycle_boot", bc).apply()
        val d = pr.getInt("cycle_boot_delay_s", 0)
        if (d > 0) Thread.sleep(d * 1000L)
        cycle(c)
    }

    private fun log(c: Context, msg: String) {
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        val old = prefs(c).getString("log", "") ?: ""
        val lines = (listOf("$t  $msg") + old.split("\n").filter { it.isNotBlank() }).take(10)
        prefs(c).edit().putString("log", lines.joinToString("\n")).apply()
    }

    fun status(c: Context): String {
        val r = c.contentResolver
        val sb = StringBuilder()
        sb.append(L.tr("Quyền WRITE_SECURE_SETTINGS: ")).append(if (hasPermission(c)) "OK" else L.tr("CHƯA CẤP")).append("\n")
        sb.append(L.tr("Thiết bị: ")).append(DeviceProfile.label()).append("\n")
        sb.append(L.tr("Trợ lý mục tiêu cho \"Gọi Gemini\": ")).append(targetLabel(c)).append(" (").append(targetPkg(c)).append(")\n")
        val cl = prefs(c).getString("chatgpt_last", "") ?: ""
        if (cl.isNotBlank()) sb.append("ChatGPT gần nhất: ").append(cl).append("\n")
        val al = prefs(c).getString("assist_last", "") ?: ""
        if (al.isNotBlank()) sb.append(L.tr("Gemini gần nhất: ")).append(al).append("\n")
        val wl = prefs(c).getString("wlog", "") ?: ""
        sb.append("\nNhật ký gọi Mi AI (mới nhất trước):\n").append(if (wl.isBlank()) "(chưa có)" else wl).append("\n")
        for (k in SECURE_KEYS) {
            val v = Settings.Secure.getString(r, k)
            sb.append(k).append(": ").append(if (v == assistComponent(c)) effectiveLabel(c) + " OK" else "[" + (v ?: L.tr("trống")) + "] " + L.tr("SAI")).append("\n")
        }
        if (DeviceProfile.isXiaomi) {
            sb.append(POWER_KEY).append(": ").append(Settings.Global.getInt(r, POWER_KEY, -1))
            val porig = powerOriginal(c)
            if (porig >= 0) sb.append(L.fmt(" (gốc đã lưu: %d)", porig))
            if (!enforcePowerKey(c)) sb.append(L.tr(" (không ghi đè)"))
            sb.append("\n")
        } else {
            sb.append(POWER_KEY).append(": ").append(L.tr("không áp dụng (chỉ Xiaomi/HyperOS)")).append("\n")
        }
        sb.append(L.tr("Trợ năng MiAiWatcher: ")).append(if (accessibilityOn(c)) L.tr("BẬT") else L.tr("TẮT")).append(if (prefs(c).getBoolean("acc_tile_off", false)) L.tr(" (tắt bằng phím tắt Control Center)") else "").append("\n")
        sb.append(L.tr("Tắt→bật Google gần nhất: ")).append(prefs(c).getString("cycle_last", "")?.ifBlank { L.tr("(chưa chạy)") } ?: L.tr("(chưa chạy)")).append("\n")
        sb.append(L.tr("\nNhật ký tự sửa (mới nhất trước):\n"))
        sb.append(prefs(c).getString("log", "")?.ifBlank { L.tr("(chưa có)") } ?: L.tr("(chưa có)"))
        return sb.toString()
    }

    /** Liet ke cac cai dat he thong lien quan tro ly / nut nguon de tim cong tac tat Mi AI. */
    fun dumpRelated(c: Context): String {
        val re = Regex("power|xiaoai|voice|assist|long_press|speech|aivoice|ai_key", RegexOption.IGNORE_CASE)
        val noise = Regex("layoutDescriptor|white_list|power_center|low_power|POWER_SAVE|perf_|powerkeep|volume_|power_supersave|superpower|lower_power", RegexOption.IGNORE_CASE)
        val sb = StringBuilder()
        val tables = listOf(
            "global" to Settings.Global.CONTENT_URI,
            "secure" to Settings.Secure.CONTENT_URI,
            "system" to Settings.System.CONTENT_URI
        )
        for ((n, u) in tables) {
            try {
                c.contentResolver.query(u, arrayOf("name", "value"), null, null, null)?.use { cur ->
                    while (cur.moveToNext()) {
                        val k = cur.getString(0) ?: continue
                        if (re.containsMatchIn(k) && !noise.containsMatchIn(k)) sb.append(n).append(": ").append(k).append(" = ").append(cur.getString(1)).append("\n")
                    }
                }
            } catch (e: Throwable) { sb.append(n).append(L.tr(": lỗi ")).append(e.message).append("\n") }
        }
        return sb.toString().ifBlank { L.tr("(không tìm thấy)") }.take(12000)
    }
}
