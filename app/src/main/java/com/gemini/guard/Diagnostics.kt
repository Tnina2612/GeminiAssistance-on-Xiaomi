package com.gemini.guard

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tong hop thong tin chan doan de nguoi dung gui kem khi can ho tro. */
object Diagnostics {
    fun build(c: Context): String {
        val pr = Restorer.prefs(c)
        val sb = StringBuilder()
        val ver = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (_: Exception) { "?" }
        val code = try {
            @Suppress("DEPRECATION")
            c.packageManager.getPackageInfo(c.packageName, 0).versionCode
        } catch (_: Exception) { -1 }

        sb.append("Gemini Guard v").append(ver).append(" (").append(code).append(")\n")
        sb.append("Thiết bị: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
            .append(" · Android ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("Nhận diện: ").append(DeviceProfile.label()).append("\n")
        sb.append("Thời điểm: ")
            .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append("\n\n")

        sb.append("=== TRẠNG THÁI ===\n").append(Restorer.status(c)).append("\n\n")
        sb.append("=== CÀI ĐẶT ĐÃ LƯU ===\n")
        for ((k, v) in pr.all.toSortedMap()) {
            if (k == "api_token") { sb.append(k).append(" = ").append(if ((v as? String).isNullOrBlank()) "(trống)" else "(đã đặt)").append("\n"); continue }
            sb.append(k).append(" = ").append(v).append("\n")
        }
        sb.append("\n=== CỬA SỔ TRỢ LÝ VỪA THẤY ===\n")
            .append(pr.getString("gseen", "")?.ifBlank { "(chưa có)" } ?: "(chưa có)").append("\n")
        sb.append("\n=== ỨNG DỤNG VỪA THẤY ===\n")
            .append(pr.getString("seen", "")?.ifBlank { "(chưa có)" } ?: "(chưa có)").append("\n")
        sb.append("\n=== CÀI ĐẶT HỆ THỐNG LIÊN QUAN ===\n").append(Restorer.dumpRelated(c)).append("\n")
        return sb.toString()
    }

    /** Mo hop thoai chia se ban chan doan (gui qua chat, email, file manager...). */
    fun share(c: Context, text: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Gemini Guard – chẩn đoán")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            c.startActivity(Intent.createChooser(i, L.tr("Chia sẻ chẩn đoán")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // Khong co app nhan: khong lam gi, nguoi dung van dung nut sao chep.
        }
    }

    /** Kiem tra nhanh cac quyen/tinh nang bat buoc, tra ve danh sach viec con thieu. */
    fun missing(c: Context): List<String> {
        val out = mutableListOf<String>()
        if (!Restorer.hasPermission(c)) out += L.tr("Quyền ghi cài đặt bảo mật (ADB)")
        if (!Settings.canDrawOverlays(c)) out += L.tr("Hiển thị trên ứng dụng khác")
        if (!Restorer.accessibilityOn(c)) out += L.tr("Trợ năng (theo dõi Mi AI)")
        return out
    }
}
