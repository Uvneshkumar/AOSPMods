package sh.siava.AOSPMods.myListeners.helper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.topjohnwu.superuser.Shell;

@SuppressLint("NotificationPermission")
public class MyBroadcastReceiver extends BroadcastReceiver {

    public static String SCREENSHOT = "uvnesh.aospmods.SCREENSHOT";
    public static String TORCH_ON = "uvnesh.aospmods.TORCH_ON";
    public static String TORCH_OFF = "uvnesh.aospmods.TORCH_OFF";
    public static String SYSTEMUI_RESTART = "uvnesh.aospmods.SYSTEMUI_RESTART";
    public static String[] actions = {SCREENSHOT, TORCH_ON, TORCH_OFF, SYSTEMUI_RESTART};

    // Send Broadcast to run through Launcher or call directly to run through any application (that has notification permission granted)
    public static void postSystemUiRestartNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "aospmods";
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "AOSP Mods",
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle("System UI is Ready")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .build();
        notificationManager.notify(2, notification);
    }

    private void postTorchNotification(Context context, boolean enabled) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (enabled) {
            String channelId = "aospmods_torch";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AOSP Mods Torch",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle("Torch on")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
            notificationManager.notify(1, notification);
        } else {
            notificationManager.cancel(1);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SCREENSHOT.equals(action)) {
            Shell.cmd("mkdir -p /sdcard/Pictures/.private", "screencap -p /sdcard/Pictures/.private/" + System.currentTimeMillis() + ".png").submit();
        } else if (TORCH_ON.equals(action)) {
            postTorchNotification(context, true);
        } else if (TORCH_OFF.equals(action)) {
            postTorchNotification(context, false);
        } else if (SYSTEMUI_RESTART.equals(action)) {
            postSystemUiRestartNotification(context);
        }
    }
}
