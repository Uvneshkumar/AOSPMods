package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

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

		if (XPrefs.Xprefs.getBoolean("requireStrongAuth", false)) {
			Class<?> LockPatternUtils = findClassIfExists("com.android.internal.widget.LockPatternUtils", lpparam.classLoader);
			if (LockPatternUtils != null) {
				tryHookAllMethods(LockPatternUtils, "requireStrongAuth", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
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
			Class<?> StrongAuthTracker = findClassIfExists("com.android.server.trust.TrustManagerService.StrongAuthTracker", lpparam.classLoader);
			if (StrongAuthTracker != null) {
				tryHookAllMethods(StrongAuthTracker, "isTrustAllowedForUser", new XC_MethodHook() {
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
						if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
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
						if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
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

			Class<?> TrustAgentService = findClassIfExists("android.service.trust.TrustAgentService", lpparam.classLoader);
			if (TrustAgentService != null) {
				tryHookAllMethods(TrustAgentService, "onTrustTimeout", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
						} catch (Exception ignored) {
						}
						param.setResult(null);
					}
				});
				tryHookAllMethods(TrustAgentService, "grantTrust", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
				});
				tryHookAllMethods(TrustAgentService, "revokeTrust", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
				tryHookAllMethods(TrustAgentService, "lockUser", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}

			Class<?> TrustAgentWrapper = findClassIfExists("com.android.server.trust.TrustAgentWrapper", lpparam.classLoader);
			if (TrustAgentWrapper != null) {
				tryHookAllMethods(TrustAgentWrapper, "onTrustTimeout", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
						} catch (Exception ignored) {
						}
						param.setResult(null);
					}
				});
			}

			Class<?> GoogleTrustAgentChimeraService = findClassIfExists("com.google.android.gms.trustagent.GoogleTrustAgentChimeraService", lpparam.classLoader);
			if (GoogleTrustAgentChimeraService != null) {
				tryHookAllMethods(GoogleTrustAgentChimeraService, "onTrustTimeout", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
						} catch (Exception ignored) {
						}
						param.setResult(null);
					}
				});
				tryHookAllMethods(GoogleTrustAgentChimeraService, "grantTrust", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
				});
				tryHookAllMethods(GoogleTrustAgentChimeraService, "revokeTrust", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
				tryHookAllMethods(GoogleTrustAgentChimeraService, "lockUser", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}

			Class<?> GoogleChimeraTrustAgentService = findClassIfExists("com.google.android.chimera.TrustAgentService", lpparam.classLoader);
			if (GoogleChimeraTrustAgentService != null) {
				tryHookAllMethods(GoogleChimeraTrustAgentService, "onTrustTimeout", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						try {
							callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
						} catch (Exception ignored) {
						}
						param.setResult(null);
					}
				});
				tryHookAllMethods(GoogleChimeraTrustAgentService, "grantTrust", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
				});
				tryHookAllMethods(GoogleChimeraTrustAgentService, "revokeTrust", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
				tryHookAllMethods(GoogleChimeraTrustAgentService, "lockUser", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}
		}
		if (XPrefs.Xprefs.getBoolean("hideNavIcons", true)) {
			Class<?> NavigationBarView = findClassIfExists("android.inputmethodservice.navigationbar.NavigationBarView", lpparam.classLoader);
			if (NavigationBarView != null) {
				tryHookAllMethods(NavigationBarView, "updateNavButtonIcons", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Object backButton = callMethod(param.thisObject, "getBackButton");
						callMethod(backButton, "setVisibility", View.INVISIBLE);
					}
				});
			}
		}
	}
}
