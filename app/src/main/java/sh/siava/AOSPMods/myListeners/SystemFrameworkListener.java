package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class SystemFrameworkListener extends XposedModPack {

	private static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

	public SystemFrameworkListener(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (XPrefs.Xprefs.getBoolean("killSystemUi", true)) {
			try {
				Class<?> PhoneWindowManager = findClassIfExists("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
				if (PhoneWindowManager != null) {
					Method powerLongPress = findMethodExactIfExists(PhoneWindowManager, "powerLongPress", long.class);
					if (powerLongPress != null) {
						hookMethod(powerLongPress, new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								if (SystemUtils.PowerManager().isInteractive() && SystemUtils.KeyguardManager().isKeyguardLocked()) {
									killSystemUi();
									param.setResult(null);
								}
							}
						});
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	private void killSystemUi() {
		try {
			Runtime rt = Runtime.getRuntime();
			String[] commands = {"pidof", "com.android.systemui"};
			Process proc = rt.exec(commands);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String s = null;
			while ((s = stdInput.readLine()) != null) {
				android.os.Process.killProcess(Integer.parseInt(s));
			}
		} catch (Throwable throwable) {
			Toast.makeText(mContext, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}