package com.mavenkalabs.adskipper.voice

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.R
import androidx.core.content.edit

class VoiceTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()

        qsTile?.let { tile ->
            val enabled = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getBoolean(getString(R.string.pref_key_pro_enable_voice), false)
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Handle the user tap
        val tile = qsTile ?: return
        if (tile.state == Tile.STATE_ACTIVE) {
            tile.state = Tile.STATE_INACTIVE
        } else {
            tile.state = Tile.STATE_ACTIVE
        }

        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit {
            putBoolean(getString(R.string.pref_key_pro_enable_voice), tile.state == Tile.STATE_ACTIVE)
        }

        tile.updateTile()
    }
}