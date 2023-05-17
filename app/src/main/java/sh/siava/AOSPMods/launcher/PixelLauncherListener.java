package sh.siava.AOSPMods.launcher;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class PixelLauncherListener extends XposedModPack {

	final boolean[] hasVibrated = {false};

	private static final String listenPackage = AOSPMods.LAUNCHER_PACKAGE;

	public PixelLauncherListener(Context context) {
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
		if (XPrefs.Xprefs.getBoolean("enablePixelVibration", false)) {
			Class<?> AllAppsRecyclerView = findClassIfExists("com.android.launcher3.allapps.AllAppsRecyclerView", lpparam.classLoader);
			if (AllAppsRecyclerView != null) {
				tryHookAllMethods(AllAppsRecyclerView, "onUpdateScrollbar", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!((View) (param.thisObject)).canScrollVertically(1) || !((View) (param.thisObject)).canScrollVertically(-1)) {
							if (!hasVibrated[0]) {
								SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
								hasVibrated[0] = true;
							}
						} else {
							hasVibrated[0] = false;
						}
					}
				});
			}
		}
	}
}