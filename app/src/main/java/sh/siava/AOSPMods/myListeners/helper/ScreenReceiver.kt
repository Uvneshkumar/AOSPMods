package sh.siava.AOSPMods.myListeners.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View

class ScreenReceiver(private val stashedHandleView: View) : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            Intent.ACTION_SCREEN_ON -> {
                stashedHandleView.scaleX = 1f
            }

            Intent.ACTION_SCREEN_OFF -> {
                stashedHandleView.scaleX = 0f
            }
        }
    }
}