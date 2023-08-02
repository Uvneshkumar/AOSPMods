package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
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

	public final int PERMISSION = 4;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (Xprefs.getBoolean("killSystemUi", true)) {
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
		if (Xprefs.getBoolean("allowDowngrade", false)) {
			Class<?> PackageManagerServiceUtilsClass = findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader);
			if (PackageManagerServiceUtilsClass != null) {
				tryHookAllMethods(PackageManagerServiceUtilsClass, "checkDowngrade", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}
		}
		if (Xprefs.getBoolean("allowMismatchedSignature", false)) {
			Class<?> SigningDetailsClass = findClassIfExists("android.content.pm.SigningDetails", lpparam.classLoader);
			Class<?> PackageManagerServiceUtilsClass = findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader);
			Class<?> InstallPackageHelperClass = findClassIfExists("com.android.server.pm.InstallPackageHelper", lpparam.classLoader);
			if (SigningDetailsClass != null) {
				tryHookAllMethods(SigningDetailsClass, "checkCapability", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!param.args[1].equals(PERMISSION)) {
							param.setResult(true);
						}
					}
				});
			}
			if (PackageManagerServiceUtilsClass != null) {
				tryHookAllMethods(PackageManagerServiceUtilsClass, "verifySignatures", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							if (callMethod(callMethod(param.args[0], "getSigningDetails"), "getSignatures") != null) {
								param.setResult(true);
							}
						} catch (Throwable ignored) {
						}
					}
				});
			}
			if (InstallPackageHelperClass != null) {
				tryHookAllMethods(InstallPackageHelperClass, "doesSignatureMatchForPermissions", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							if (callMethod(param.args[1], "getPackageName").equals(param.args[0]) && ((String) callMethod(param.args[1], "getBaseApkPath")).startsWith("/data")) {
								param.setResult(true);
							}
						} catch (Throwable ignored) {
						}
					}
				});
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