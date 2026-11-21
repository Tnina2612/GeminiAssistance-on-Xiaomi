package com.gemini.guard

/**
 * Loc cua so Mi AI / YOYO. Tach rieng thanh ham thuan (khong phu thuoc Android)
 * de co unit test va de moi noi dung cung mot quy tac.
 */
object Filters {
    /** Tach danh sach nhap tay: phay, cham phay, xuong dong hoac khoang trang. */
    fun split(list: String): List<String> =
        list.split(",", ";", "\n", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Ten cua so co chua bat ky muc nao trong danh sach (khong phan biet hoa thuong). */
    fun matches(list: String, cls: String): Boolean {
        if (list.isBlank() || cls.isBlank()) return false
        return split(list).any { cls.contains(it, ignoreCase = true) }
    }

    /**
     * Quyet dinh co goi tro ly hay khong:
     * - thuoc danh sach bo qua (exclude) -> false
     * - danh sach can co (include) khac rong va khong khop -> false
     * - con lai -> true
     */
    fun allowed(include: String, exclude: String, cls: String): Boolean {
        if (exclude.isNotBlank() && matches(exclude, cls)) return false
        if (include.isNotBlank() && !matches(include, cls)) return false
        return true
    }
}
