package com.gemini.guard

import android.os.Build
import java.util.Locale

/**
 * Nhan dien nhanh phan mem cua may de bat/tat cac tinh nang dac thu.
 *
 * - Xiaomi/HyperOS: ghi duoc power_button_long_press, co Mi AI, app Dich cua Xiaomi.
 * - Honor/MagicOS: khong co power_button_long_press; tro ly la YOYO (com.hihonor.assistant /
 *   com.hihonor.magicvoice). Cac phan con lai dung API chuan Android.
 * - Khac: chi dung phan loi (giu tro ly mac dinh, thanh noi, tro nang).
 */
enum class Vendor { XIAOMI, HONOR, OTHER }

object DeviceProfile {
    val vendor: Vendor = detect()

    val isXiaomi: Boolean get() = vendor == Vendor.XIAOMI
    val isHonor: Boolean get() = vendor == Vendor.HONOR

    /** Ten ngan gon de hien thi trong phan chan doan. */
    fun label(): String = when (vendor) {
        Vendor.XIAOMI -> "Xiaomi / HyperOS"
        Vendor.HONOR -> "Honor / MagicOS"
        else -> {
            val m = Build.MANUFACTURER?.trim().orEmpty()
            if (m.isEmpty()) "Android" else m.replaceFirstChar { it.uppercase(Locale.US) } + " / Android"
        }
    }

    private fun detect(): Vendor {
        val m = (Build.MANUFACTURER.orEmpty() + " " + Build.BRAND.orEmpty()).lowercase(Locale.US)
        return when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> Vendor.XIAOMI
            m.contains("honor") || m.contains("hihonor") -> Vendor.HONOR
            else -> Vendor.OTHER
        }
    }
}
