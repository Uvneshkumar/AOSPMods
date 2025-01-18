package sh.siava.AOSPMods.myListeners;

import static android.os.VibrationAttributes.USAGE_ACCESSIBILITY;
import static android.os.VibrationAttributes.USAGE_TOUCH;
import static android.os.VibrationEffect.EFFECT_TICK;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;
import static sh.siava.AOSPMods.utils.SystemUtils.ToggleFlash;
import static sh.siava.AOSPMods.utils.SystemUtils.vibrate;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
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
	public static final int WAKE_REASON_POWER_BUTTON = 1;

	private boolean isVolDown = false;
	private long mWakeTime = 0;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (Xprefs.getBoolean("killSystemUi", false)) {
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
		if (Xprefs.getBoolean("holdVolumeToSkipMusic", false)) {
			Class<?> PhoneWindowManager = findClassIfExists("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
			if (PhoneWindowManager != null) {
				Method interceptKeyBeforeQueueing = findMethodExactIfExists(PhoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class);
				if (interceptKeyBeforeQueueing != null) {
					Runnable mVolumeLongPress = () -> {
						try {
							Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
							KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, (isVolDown) ? KeyEvent.KEYCODE_MEDIA_PREVIOUS : KeyEvent.KEYCODE_MEDIA_NEXT, 0);
							keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
							SystemUtils.AudioManager().dispatchMediaKeyEvent(keyEvent);
							keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
							keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
							SystemUtils.AudioManager().dispatchMediaKeyEvent(keyEvent);
							vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
						} catch (Throwable ignored) {
						}
					};
					hookMethod(interceptKeyBeforeQueueing, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							try {
								Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
								KeyEvent e = (KeyEvent) param.args[0];
								int Keycode = e.getKeyCode();
								switch (e.getAction()) {
									case KeyEvent.ACTION_UP:
										if (mHandler.hasCallbacks(mVolumeLongPress)) {
											SystemUtils.AudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, Keycode == KeyEvent.KEYCODE_VOLUME_DOWN ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);
											mHandler.removeCallbacks(mVolumeLongPress);
										}
										return;
									case KeyEvent.ACTION_DOWN:
										if (!SystemUtils.PowerManager().isInteractive() && (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN || Keycode == KeyEvent.KEYCODE_VOLUME_UP) && SystemUtils.AudioManager().isMusicActive()) {
											isVolDown = (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN);
											mHandler.postDelayed(mVolumeLongPress, ViewConfiguration.getLongPressTimeout());
											param.setResult(0);
										}
								}
							} catch (Throwable ignored) {
							}
						}
					});
				}
			}
		}
		if (Xprefs.getBoolean("holdPowerForTorch", false)) {
			Class<?> PhoneWindowManagerClass = findClassIfExists("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
			Class<?> PowerKeyRuleClass = findClassIfExists("com.android.server.policy.PhoneWindowManager$PowerKeyRule", lpparam.classLoader);
			hookAllMethods(PhoneWindowManagerClass, "startedWakingUp", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((int) param.args[param.args.length - 1] == WAKE_REASON_POWER_BUTTON) {
						mWakeTime = SystemClock.uptimeMillis();
					}
				}
			});
			hookAllMethods(PowerKeyRuleClass, "onLongPress", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					boolean screenIsOn = screenIsOn();
					if (!screenIsOn) {
						launchAction();
						param.setResult(null);
					} else if (Xprefs.getBoolean("holdPowerForTorchEverywhere", false)) {
						ToggleFlash();
						vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);
						param.setResult(null);
					}
				}
			});
		}
		if (Xprefs.getBoolean("vibrationTouchAlways", false)) {
			Class<?> VibrationAttributes = findClassIfExists("android.os.VibrationAttributes", lpparam.classLoader);
			if (VibrationAttributes != null) {
				tryHookAllMethods(VibrationAttributes, "createForUsage", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.args[0] = USAGE_TOUCH;
					}
				});
			}
		}
	}

	private void launchAction() {
		try {
			ToggleFlash();
			vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);
			new Thread(SystemUtils::Sleep).start();
		} catch (Throwable ignored) {
		}
	}

	private boolean screenIsOn() { //for power button, display state isn't reliable enough because pressing power will trigger it
		return SystemClock.uptimeMillis() - mWakeTime > 1000;
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