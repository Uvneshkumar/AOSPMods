package sh.siava.AOSPMods.myListeners.helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.topjohnwu.superuser.Shell;

import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressLint("NotificationPermission")
public class MyBroadcastReceiver extends BroadcastReceiver {

    public static String SCREENSHOT = "uvnesh.aospmods.SCREENSHOT";
    public static String ADVANCED_REBOOT = "uvnesh.aospmods.ADVANCED_REBOOT";
    public static String TORCH_ON = "uvnesh.aospmods.TORCH_ON";
    public static String TORCH_OFF = "uvnesh.aospmods.TORCH_OFF";
    public static String TORCH_ACTUAL_OFF = "uvnesh.aospmods.TORCH_ACTUAL_OFF";
    public static String SYSTEMUI_RESTART = "uvnesh.aospmods.SYSTEMUI_RESTART";
    public static String SYSTEMUI_RESTART_DISMISS = "uvnesh.aospmods.SYSTEMUI_RESTART_DISMISS";
    public static String GET_TEMPERATURE = "uvnesh.aospmods.GET_TEMPERATURE";
    public static String[] actions = {SCREENSHOT, GET_TEMPERATURE, ADVANCED_REBOOT};

    public static String SEND_TEMPERATURE = "uvnesh.aospmods.SEND_TEMPERATURE";
    public static String[] SystemUIActions = {SEND_TEMPERATURE, TORCH_ON, TORCH_OFF, TORCH_ACTUAL_OFF, SYSTEMUI_RESTART, SYSTEMUI_RESTART_DISMISS};

    public CustomDateAlarmLayout customDateAlarmLayoutSmall;
    public CustomDateAlarmLayout customDateAlarmLayoutBig;

    public void postSystemUiRestartNotification(Context context, boolean dismiss) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (dismiss) {
            notificationManager.cancel(2);
        } else {
            String channelId = "aospmods";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AOSP Mods",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
            Intent dismissIntent = new Intent(SYSTEMUI_RESTART_DISMISS);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Notification notification = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle("System UI is ready")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .addAction(
                            android.R.drawable.sym_def_app_icon,
                            "Dismiss",
                            dismissPendingIntent
                    )
                    .build();
            notificationManager.notify(2, notification);
        }
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
            Intent dismissIntent = new Intent(TORCH_ACTUAL_OFF);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Notification notification = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle("Torch on")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .addAction(
                            android.R.drawable.sym_def_app_icon,
                            "Turn off",
                            dismissPendingIntent
                    )
                    .build();
            notificationManager.notify(1, notification);
        } else {
            notificationManager.cancel(1);
        }
    }

    Bitmap leftBitmap;
    String temperature;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SCREENSHOT.equals(action)) {
            Shell.cmd("mkdir -p /sdcard/Pictures/.private", "screencap -p /sdcard/Pictures/.private/" + System.currentTimeMillis() + ".png").submit();
        } else if (TORCH_ON.equals(action)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                postTorchNotification(context, true);
            }, 100);
        } else if (TORCH_OFF.equals(action)) {
            postTorchNotification(context, false);
        } else if (TORCH_ACTUAL_OFF.equals(action)) {
            SystemUtils.TurnOffFlash();
        } else if (SYSTEMUI_RESTART.equals(action)) {
            postSystemUiRestartNotification(context, false);
        } else if (SYSTEMUI_RESTART_DISMISS.equals(action)) {
            postSystemUiRestartNotification(context, true);
        } else if (GET_TEMPERATURE.equals(action)) {
            sendTemperature(context);
        } else if (ADVANCED_REBOOT.equals(action)) {
            showAdvancedRebootMenu(context);
        } else if (SEND_TEMPERATURE.equals(action)) {
            if (customDateAlarmLayoutSmall != null) {
                Bitmap leftBitmap = intent.getParcelableExtra("leftBitmap", Bitmap.class);
                String temperature = intent.getStringExtra("temperature");
                if (leftBitmap != null && temperature != null && !temperature.isEmpty()) {
                    Drawable leftDrawable = new BitmapDrawable(context.getResources(), leftBitmap);
                    customDateAlarmLayoutSmall.setTemperature(temperature, leftDrawable);
                    if (customDateAlarmLayoutBig != null) {
                        customDateAlarmLayoutBig.setTemperature(temperature, leftDrawable);
                    }
                }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void showAdvancedRebootMenu(Context context) {
        String[] options = {"Reboot System", "Recovery", "Bootloader", "Fastboot", "Download", "EDL", "Restart Launcher", "Restart SystemUI"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.ThemeOverlay_Material_Dialog_Alert);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    SystemUtils.Restart();
                    break;
                case 1:
                    SystemUtils.reboot("recovery");
                    break;
                case 2:
                    SystemUtils.reboot("bootloader");
                    break;
                case 3:
                    SystemUtils.reboot("fastboot");
                    break;
                case 4:
                    SystemUtils.reboot("download");
                    break;
                case 5:
                    SystemUtils.reboot("edl");
                    break;
                case 6:
                    SystemUtils.RestartLauncher();
                    break;
                case 7:
                    SystemUtils.RestartSystemUI();
                    break;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        dialog.show();
    }

    public void setTemperatureDataAndSend(Bitmap leftBitmap, String temperature, Context context) {
        this.leftBitmap = leftBitmap;
        this.temperature = temperature;
        sendTemperature(context);
    }

    public void sendTemperature(Context context) {
        if (leftBitmap != null && temperature != null && !temperature.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(MyBroadcastReceiver.SEND_TEMPERATURE);
            intent.putExtra("leftBitmap", leftBitmap);
            intent.putExtra("temperature", temperature);
            context.sendBroadcast(intent);
        }
    }
}
