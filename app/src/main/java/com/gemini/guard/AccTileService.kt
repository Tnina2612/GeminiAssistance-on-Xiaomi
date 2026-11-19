package com.gemini.guard

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Phim tat o Control Center: bat / tat rieng dich vu Tro nang cua Gemini Guard bang tay,
 * de tat truoc khi mo app ngan hang / VNeID (cac app do quet danh sach Tro nang luc khoi dong).
 * Dung quyen WRITE_SECURE_SETTINGS nen khong can vao Cai dat.
 */
class AccTileService : TileService() {

    override fun onStartListening() { refresh() }
    override fun onTileAdded() { refresh() }

    override fun onClick() {
        val c = applicationContext
        val msg = AccToggle.toggle(c)
        if (!Restorer.hasPermission(c)) Toast.makeText(c, msg, Toast.LENGTH_LONG).show()
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        val c = applicationContext
        val perm = Restorer.hasPermission(c)
        val on = perm && AccToggle.isOn(c)
        t.state = when {
            !perm -> Tile.STATE_UNAVAILABLE
            on -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        t.label = L.tr("Trợ năng Gemini Guard")
        if (Build.VERSION.SDK_INT >= 29) {
            t.subtitle = when {
                !perm -> L.tr("Chưa cấp quyền")
                on -> L.tr("Đang bật")
                else -> L.tr("Đang tắt")
            }
        }
        t.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        t.updateTile()
    }
}
