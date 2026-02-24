package sh.siava.AOSPMods;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XModuleResources;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.IXposedHookZygoteInit;


public class XPrefs implements IXposedHookZygoteInit {

    public static String MOD_PATH = "";
    public static XModuleResources modRes;
    public static SharedPreferences Xprefs;
    private static String packageName;

    static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(packageName, key);

    public static void init(Context context) {
        packageName = context.getPackageName();
        Xprefs = new RemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
        log("AOSPMods Version: " + BuildConfig.VERSION_NAME);
        log("AOSPMods Records: " + Xprefs.getAll().keySet().size());
        Xprefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void loadEverything(String packageName, String... key) {
        for (XposedModPack thisMod : AOSPMods.runningMods) {
            thisMod.updatePrefs(key);
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MOD_PATH = startupParam.modulePath;
        modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);
    }
}
