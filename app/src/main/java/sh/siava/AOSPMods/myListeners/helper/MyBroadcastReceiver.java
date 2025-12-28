package sh.siava.AOSPMods.myListeners.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.topjohnwu.superuser.Shell;

public class MyBroadcastReceiver extends BroadcastReceiver {

    public static String SCREENSHOT = "uvnesh.aospmods.SCREENSHOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SCREENSHOT.equals(action)) {
            Shell.cmd("mkdir -p /sdcard/Pictures/.private", "screencap -p /sdcard/Pictures/.private/" + System.currentTimeMillis() + ".png").submit();
        }
    }
}
