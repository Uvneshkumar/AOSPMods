package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class TelecomListener extends XposedModPack {

	private static final String listenPackage = AOSPMods.TELECOM_SERVER_PACKAGE;

	private final VibrationEffect vibrationEffect = VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, -1); //100ms on, 100 off, 100 again. don't repeat
	private long lastActiveVibration = 0, lastDropVibration = 0;
	private boolean vibrateOnAnswered = false, vibrateOnDrop = false;

	public TelecomListener(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		vibrateOnAnswered = Xprefs.getBoolean("vibrateOnCallAnswered", false);
		vibrateOnDrop = Xprefs.getBoolean("vibrateOnCallDisconnected", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		try {
			Class<?> InCallControllerClass = findClassIfExists("com.android.server.telecom.InCallController", lpparam.classLoader);
			if (InCallControllerClass == null) return;
			tryHookAllMethods(InCallControllerClass, "onCallStateChanged", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						int oldState = (int) param.args[1];
						int newState = (int) param.args[2];
						if (vibrateOnAnswered
								&& oldState == 3 /* Dialing */
								&& newState == 5 /* ACTIVE */
								&& SystemClock.uptimeMillis() - lastActiveVibration > 5000L /* Don't vibrate on concurrent method calls */) {
							lastActiveVibration = SystemClock.uptimeMillis();
							SystemUtils.vibrate(vibrationEffect, VibrationAttributes.USAGE_NOTIFICATION);
						} else if (vibrateOnDrop
								&& oldState == 5 /* ACTIVE */
								&& newState == 7 /* DISCONNECTED */
								&& SystemClock.uptimeMillis() - lastDropVibration > 5000L) {
							lastDropVibration = SystemClock.uptimeMillis();
							SystemUtils.vibrate(vibrationEffect, VibrationAttributes.USAGE_NOTIFICATION);
						}
					} catch (Throwable ignored) {
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}
}