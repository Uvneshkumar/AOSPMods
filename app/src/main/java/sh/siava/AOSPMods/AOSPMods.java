package sh.siava.AOSPMods;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.myListeners.AllAppsListener;
import sh.siava.AOSPMods.myListeners.LauncherListener;
import sh.siava.AOSPMods.myListeners.QSQuickPullDown;
import sh.siava.AOSPMods.myListeners.SystemFrameworkListener;
import sh.siava.AOSPMods.myListeners.SystemUIListener;
import sh.siava.AOSPMods.myListeners.TelecomListener;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class AOSPMods implements IXposedHookLoadPackage {
    public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    public static final String SYSTEM_FRAMEWORK_PACKAGE = "android";
    public static final String TELECOM_SERVER_PACKAGE = "com.android.server.telecom";

    public static boolean isChildProcess = false;

    public static ArrayList<Class<?>> modPacks = new ArrayList<>();
    public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
    public Context mContext = null;

    public AOSPMods() {
        modPacks.add(QSQuickPullDown.class);
        modPacks.add(AllAppsListener.class);
        modPacks.add(LauncherListener.class);
        modPacks.add(SystemUIListener.class);
        modPacks.add(SystemFrameworkListener.class);
        modPacks.add(TelecomListener.class);
    }

    @SuppressLint("ApplySharedPref")
    private static boolean bootLooped(String packageName) {
        String loadTimeKey = String.format("packageLastLoad_%s", packageName);
        String strikeKey = String.format("packageStrike_%s", packageName);
        long currentTime = Calendar.getInstance().getTime().getTime();
        long lastLoadTime = Xprefs.getLong(loadTimeKey, 0);
        int strikeCount = Xprefs.getInt(strikeKey, 0);
        if (currentTime - lastLoadTime > 40000) {
            Xprefs.edit().putLong(loadTimeKey, currentTime).putInt(strikeKey, 0).commit();
        } else if (strikeCount >= 3) {
            return true;
        } else {
            Xprefs.edit().putInt(strikeKey, ++strikeCount).commit();
        }
        return false;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            isChildProcess = lpparam.processName.contains(":");
        } catch (Exception ignored) {
        }
        findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mContext == null) {
                    mContext = (Context) param.args[2];
                    XPrefs.init(mContext);
                    if (bootLooped(mContext.getPackageName())) {
                        log(String.format("AOSPMods: Possible bootloop in %s. Will not load for now", mContext.getPackageName()));
                        return;
                    }
                    new SystemUtils(mContext);
                    XPrefs.loadEverything(mContext.getPackageName());
                }
                for (Class<?> mod : modPacks) {
                    try {
                        XposedModPack instance = ((XposedModPack) mod.getConstructor(Context.class).newInstance(mContext));
                        if (!instance.listensTo(lpparam.packageName)) continue;
                        try {
                            instance.updatePrefs();
                        } catch (Throwable ignored) {
                        }
                        instance.handleLoadPackage(lpparam);
                        runningMods.add(instance);
                    } catch (Throwable T) {
                        log("Start Error Dump - Occurred in " + mod.getName());
                        T.printStackTrace();
                    }
                }
            }
        });

    }
}