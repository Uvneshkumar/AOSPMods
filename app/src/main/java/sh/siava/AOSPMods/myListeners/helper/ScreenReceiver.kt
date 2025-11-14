package sh.siava.AOSPMods.myListeners.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import com.topjohnwu.superuser.Shell

class ScreenReceiver(
    private val stashedHandleView: View,
    private val enable_taskbar_on_phones: Boolean,
    private val isBatterySaverOnScreenOff: Boolean
) : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            Intent.ACTION_SCREEN_ON -> {
                if (enable_taskbar_on_phones) {
                    stashedHandleView.scaleX = 1f
                }
                if (isBatterySaverOnScreenOff) {
                    Shell.cmd("svc wifi enable").exec()
                    Shell.cmd("cmd bluetooth_manager enable").exec()
                    Shell.cmd("settings put global low_power 0").exec()
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                if (enable_taskbar_on_phones) {
                    stashedHandleView.scaleX = 0f
                }
                if (isBatterySaverOnScreenOff) {
                    Shell.cmd("svc wifi disable").exec()
                    Shell.cmd("cmd bluetooth_manager disable").exec()
                    Shell.cmd("settings put global low_power 1").exec()
                }
            }
        }
    }
}