package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.graphics.Color;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class AllAppsListener extends XposedModPack {

	int STRONG_AUTH_NOT_REQUIRED = 0x0;

	public AllAppsListener(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

	}

	@Override
	public boolean listensTo(String packageName) {
		return true;
	} //This mod is compatible with every package

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		// Disable (Pixel) Launcher Top Shadow
		Class<?> SysUiScrim = findClassIfExists("com.android.launcher3.graphics.SysUiScrim", lpparam.classLoader);
		if (SysUiScrim != null) {
			tryHookAllMethods(SysUiScrim, "draw", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(null);
				}
			});
		}

		// https://github.com/AidanWarner97/frameworks_base/commit/cee0f07dad38c2dc93717548da8924b0c0989e64
		if (XPrefs.Xprefs.getBoolean("whiteBatteryIcon", false)) {
			Class<?> ThemedBatteryDrawable = findClassIfExists("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader);
			if (ThemedBatteryDrawable != null) {
				tryHookAllConstructors(ThemedBatteryDrawable, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setIntField(param.thisObject, "fillColor", Color.WHITE);
						setIntField(param.thisObject, "backgroundColor", Color.WHITE);
						setIntField(param.thisObject, "levelColor", Color.WHITE);
					}
				});
			}
		}

		Class<?> LockPatternUtils = findClassIfExists("com.android.internal.widget.LockPatternUtils", lpparam.classLoader);
		if (LockPatternUtils != null) {
			tryHookAllMethods(LockPatternUtils, "requireStrongAuth", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
						param.setResult(null);
					}
				}
			});
			tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(STRONG_AUTH_NOT_REQUIRED);
				}
			});
			tryHookAllMethods(LockPatternUtils, "isTrustAllowedForUser", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(true);
				}
			});
		}
		Class<?> LockSettingsService = findClassIfExists("com.android.server.locksettings.LockSettingsService", lpparam.classLoader);
		if (LockSettingsService != null) {
			tryHookAllMethods(LockSettingsService, "requireStrongAuth", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
						param.setResult(null);
					}
				}
			});
			tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(STRONG_AUTH_NOT_REQUIRED);
				}
			});
		}
		Class<?> LockSettingsStrongAuth = findClassIfExists("com.android.server.locksettings.LockSettingsStrongAuth", lpparam.classLoader);
		if (LockSettingsStrongAuth != null) {
			tryHookAllMethods(LockSettingsStrongAuth, "handleScheduleNonStrongBiometricIdleTimeout", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(null);
				}
			});
			tryHookAllMethods(LockSettingsStrongAuth, "requireStrongAuth", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
						param.setResult(null);
					}
				}
			});
			tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.setResult(STRONG_AUTH_NOT_REQUIRED);
				}
			});
		}
	}
}
