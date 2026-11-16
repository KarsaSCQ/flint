package io.github.yizhu.flint.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.yizhu.flint.MainActivity

/**
 * Quick-settings tile: tap -> Flint search opens, keyboard up.
 * The service only lives for the tap moment; zero residency.
 */
class FlintTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.state = Tile.STATE_ACTIVE
            it.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}
