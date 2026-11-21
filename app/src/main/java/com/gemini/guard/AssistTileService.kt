package com.gemini.guard

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** O Control Center goi thang tro ly muc tieu (Gemini / ChatGPT / goi khac). */
class AssistTileService : TileService() {
    override fun onStartListening() { refresh() }
    override fun onTileAdded() { refresh() }

    override fun onClick() {
        val c = applicationContext
        if (Restorer.prefs(c).getBoolean("enabled", true)) Restorer.ensure(c)
        val err = Restorer.callTarget(c)
        if (err == null) GeminiSound.play(c)
        else Toast.makeText(c, L.fmt("Không gọi được %s: %s", Restorer.targetLabel(c), err), Toast.LENGTH_LONG).show()
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        t.state = Tile.STATE_INACTIVE
        t.label = L.tr("Gọi trợ lý")
        t.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        t.updateTile()
    }
}
