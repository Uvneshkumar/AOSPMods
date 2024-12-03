package sh.siava.AOSPMods

import android.app.Application
import sh.siava.AOSPMods.myListeners.helper.Helper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Helper.loadAppIcons(this)
    }
}