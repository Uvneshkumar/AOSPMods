package sh.siava.AOSPMods.myListeners;

import android.content.Context;

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

	}
}
