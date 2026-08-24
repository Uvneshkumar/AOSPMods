package sh.siava.AOSPMods.utils;

import static com.topjohnwu.superuser.Shell.cmd;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import org.jetbrains.annotations.Contract;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.myListeners.helper.MyBroadcastReceiver;

public class SystemUtils {
    private static final int THREAD_PRIORITY_BACKGROUND = 10;

    @SuppressLint("StaticFieldLeak")
    static SystemUtils instance;
    Context mContext;
    CameraManager mCameraManager;
    VibratorManager mVibrationManager;
    AudioManager mAudioManager;
    PowerManager mPowerManager;
    KeyguardManager mKeyguardManager;
    boolean hasVibrator;
    TorchCallback torchCallback = new TorchCallback();
    private Handler mHandler = null;

    public SystemUtils(Context context) {
        mContext = context;
        instance = this;
        try {
            mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                log("AOSPMods Error getting power manager");
                t.printStackTrace();
            }
        }
        try {
            mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                log("AOSPMods Error getting keyguard manager");
                t.printStackTrace();
            }
        }
        try {
            mVibrationManager = (VibratorManager) mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            hasVibrator = mVibrationManager.getDefaultVibrator().hasVibrator();
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                log("AOSPMods Error getting vibrator");
                t.printStackTrace();
            }
        }
    }

    public static void RestartSystemUI() {
        cmd("killall com.android.systemui").submit();
    }

    public static void RestartLauncher() {
        cmd("killall com.android.launcher3; killall com.android.launcher; killall com.google.android.apps.nexuslauncher; killall com.sec.android.app.launcher").submit();
    }

    public static void Restart() {
        cmd("am start -a android.intent.action.REBOOT").submit();
    }

    // https://github.com/topjohnwu/Magisk/blob/master/app/core/src/main/java/com/topjohnwu/magisk/core/ktx/XSU.kt
    public static void reboot(String reason) {
        cmd("/system/bin/svc power reboot " + reason + " || /system/bin/reboot " + reason).submit();
    }

    public static void ToggleFlash() {
        if (instance == null) return;
        instance.toggleFlashInternal();
    }

    public static void TurnOffFlash() {
        if (instance == null) return;
        instance.turnOffFlashInternal();
    }

    @Nullable
    @Contract(pure = true)
    public static AudioManager AudioManager() {
        if (instance == null) return null;
        return instance.getAudioManager();
    }

    @Nullable
    @Contract(pure = true)
    public static PowerManager PowerManager() {
        if (instance == null) return null;
        return instance.mPowerManager;
    }

    @Nullable
    @Contract(pure = true)
    public static KeyguardManager KeyguardManager() {
        if (instance == null) return null;
        return instance.mKeyguardManager;
    }

    public static void vibrate(int effect, @Nullable Integer vibrationUsage) {
        vibrate(VibrationEffect.createPredefined(effect), vibrationUsage);
    }

    @SuppressLint("MissingPermission")
    public static void vibrate(VibrationEffect effect, @Nullable Integer vibrationUsage) {
        if (instance == null || !instance.hasVibrator) return;
        try {
            if (vibrationUsage != null) {
                instance.mVibrationManager.getDefaultVibrator().vibrate(effect, VibrationAttributes.createForUsage(vibrationUsage));
            } else {
                instance.mVibrationManager.getDefaultVibrator().vibrate(effect);
            }
        } catch (Exception ignored) {
        }
    }

    public static void Sleep() {
        if (instance == null || Xprefs.getBoolean("rootForSleep", false)) {
            rootSleep();
        } else {
            try {
                callMethod(instance.mPowerManager, "goToSleep", SystemClock.uptimeMillis());
            } catch (Throwable throwable) {
                rootSleep();
            }
        }
    }

    private static void rootSleep() {
        Shell.cmd("input keyevent " + KeyEvent.KEYCODE_SLEEP).submit();
    }

    private AudioManager getAudioManager() { //we don't init audio manager unless it's requested by someone
        if (mAudioManager == null) {
            //Audio
            try {
                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            } catch (Throwable t) {
                if (BuildConfig.DEBUG) {
                    t.printStackTrace();
                }
            }
        }
        return mAudioManager;
    }

    private void setFlashInternal(boolean enabled) {
        if (cantInitCamera()) {
            return;
        }
        try {
            String flashID = getFlashID(mCameraManager);
            if (flashID.equals("")) {
                return;
            }
            mCameraManager.setTorchMode(flashID, enabled);
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                log("AOSPMods Error in setting flashlight");
                t.printStackTrace();
            }
        }
        Intent intent = new Intent();
        if (enabled) {
            intent.setAction(MyBroadcastReceiver.TORCH_ON);
        } else {
            intent.setAction(MyBroadcastReceiver.TORCH_OFF);
        }
        mContext.sendBroadcast(intent);
    }

    private boolean cantInitCamera() {
        if (Xprefs.getBoolean("disableCameraService", false)) return true;
        if (mCameraManager != null) return false;
        try {
            HandlerThread thread = new HandlerThread("", THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mHandler = new Handler(thread.getLooper());
            mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            mCameraManager.registerTorchCallback(torchCallback, mHandler);
            return false;
        } catch (Throwable t) {
            mCameraManager = null;
            if (BuildConfig.DEBUG) {
                t.printStackTrace();
            }
            return true;
        }
    }

    private void toggleFlashInternal() {
        setFlashInternal(!TorchCallback.torchOn);
    }

    private void turnOffFlashInternal() {
        setFlashInternal(false);
    }

    private String getFlashID(@NonNull CameraManager cameraManager) throws CameraAccessException {
        String[] ids = cameraManager.getCameraIdList();
        for (String id : ids) {
            if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                    return id;
                }
            }
        }
        return "";
    }

    static class TorchCallback extends CameraManager.TorchCallback {
        static boolean torchOn = false;

        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            torchOn = enabled;
        }
    }
}