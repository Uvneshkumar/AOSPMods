package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.graphics.Color;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class SystemUIListener extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	public SystemUIListener(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;
		if (Xprefs.getBoolean("disableLocationPrivacyIndicator", false)) {
			Class<?> PrivacyConfig = findClassIfExists("com.android.systemui.privacy.PrivacyConfig", lpparam.classLoader);
			if (PrivacyConfig != null) {
				tryHookAllMethods(PrivacyConfig, "isLocationEnabled", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(false);
					}
				});
			}
		}
		if (Xprefs.getBoolean("disableLockScreenBounce", false)) {
			Class<?> NotificationPanelViewController = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
			if (NotificationPanelViewController != null) {
				tryHookAllMethods(NotificationPanelViewController, "startUnlockHintAnimation", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						callMethod(param.thisObject, "onUnlockHintStarted");
						callMethod(param.thisObject, "onUnlockHintFinished");
						param.setResult(null);
					}
				});
			}
		}
		if (Xprefs.getBoolean("disableBluetoothIcon", false)) {
			Class<?> PhoneStatusBarPolicy = findClassIfExists("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", lpparam.classLoader);
			if (PhoneStatusBarPolicy != null) {
				tryHookAllMethods(PhoneStatusBarPolicy, "updateBluetooth", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}
		}
		if (Xprefs.getBoolean("disableBatteryTime", false)) {
			Class<?> BatteryControllerImpl = findClassIfExists("com.android.systemui.statusbar.policy.BatteryControllerImpl", lpparam.classLoader);
			if (BatteryControllerImpl != null) {
				tryHookAllMethods(BatteryControllerImpl, "generateTimeRemainingString", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
					}
				});
			}
		}
		if (Xprefs.getBoolean("hookUnlockAnim", false)) {
			Class<?> KeyguardUnlockAnimationController = findClassIfExists("com.android.systemui.keyguard.KeyguardUnlockAnimationController", lpparam.classLoader);
			if (KeyguardUnlockAnimationController != null) {
				tryHookAllMethods(KeyguardUnlockAnimationController, "canPerformInWindowLauncherAnimations", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(false);
					}
				});
			}
		}
		if (Xprefs.getBoolean("whiteLockClock", false)) {
			Class<?> AnimatableClockView = findClassIfExists("com.android.systemui.shared.clocks.AnimatableClockView", lpparam.classLoader);
			if (AnimatableClockView != null) {
				tryHookAllMethods(AnimatableClockView, "animateAppearOnLockscreen", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setIntField(param.thisObject, "lockScreenColor", Color.WHITE);
					}
				});
				tryHookAllMethods(AnimatableClockView, "animateFoldAppear", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setIntField(param.thisObject, "lockScreenColor", Color.WHITE);
					}
				});
				tryHookAllMethods(AnimatableClockView, "animateDoze", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setIntField(param.thisObject, "lockScreenColor", Color.WHITE);
					}
				});
			}
		}
		if (Xprefs.getBoolean("hideLockScreenStatusBar", false)) {
			Class<?> KeyguardStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);
			if (KeyguardStatusBarView != null) {
				tryHookAllMethods(KeyguardStatusBarView, "loadDimens", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View mSystemIconsContainer = (View) getObjectField(param.thisObject, "mSystemIconsContainer");
						callMethod(mSystemIconsContainer, "setVisibility", View.INVISIBLE);
					}
				});
			}
		}
		if (Xprefs.getBoolean("forceSmallClock", false)) {
			Class<?> KeyguardClockSwitch = findClassIfExists("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader);
			if (KeyguardClockSwitch != null) {
				tryHookAllMethods(KeyguardClockSwitch, "animateClockChange", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.args[0] = false;
					}
				});
			}
		}
		if (Xprefs.getBoolean("hideLockIcon", false)) {
			Class<?> LockIconViewController = findClassIfExists("com.android.keyguard.LockIconViewController", lpparam.classLoader);
			if (LockIconViewController != null) {
				tryHookAllMethods(LockIconViewController, "updateVisibility", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						View mSystemIconsContainer = (View) getObjectField(param.thisObject, "mView");
						callMethod(mSystemIconsContainer, "setVisibility", View.INVISIBLE);
						param.setResult(null);
					}
				});
			}
		}
		if (Xprefs.getBoolean("qsTileVibrate", false)) {
			Class<?> QSTileImplClass = findClassIfExists("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);
			if (QSTileImplClass != null) {
				XC_MethodHook vibrateCallback = new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
					}
				};
				tryHookAllMethods(QSTileImplClass, "click", vibrateCallback);
				tryHookAllMethods(QSTileImplClass, "longClick", vibrateCallback);
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
	}
}
