package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class AllAppsListener extends XposedModPack {

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
		Class<?> ThemesClass = findClassIfExists("com.android.launcher3.util.Themes", lpparam.classLoader);
		if (ThemesClass != null) {
			tryHookAllMethods(ThemesClass, "getAttrDrawable", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((int) param.args[1] != 16843534) {
						param.setResult(null);
					}
				}
			});
		}
	}
}
