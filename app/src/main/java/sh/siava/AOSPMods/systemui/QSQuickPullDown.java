package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSQuickPullDown extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private static final int PULLDOWN_SIDE_RIGHT = 1;
	@SuppressWarnings("unused")
	private static final int PULLDOWN_SIDE_LEFT = 2;
	private static final int STATUSBAR_MODE_SHADE = 0;

	private static int pullDownSide = PULLDOWN_SIDE_RIGHT;
	private static boolean oneFingerPulldownEnabled = false;
	private static float statusbarPortion = 0.50f; // now set to 50% of the screen. it can be anything between 0 to 100%

	public QSQuickPullDown(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		oneFingerPulldownEnabled = Xprefs.getBoolean("QSPulldownEnabled", false);
		statusbarPortion = Xprefs.getInt("QSPulldownPercent", 50) / 100f;
		pullDownSide = Integer.parseInt(Xprefs.getString("QSPulldownSide", "1"));
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;
		Class<?> NotificationPanelViewControllerClass = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
		if (findFieldIfExists(NotificationPanelViewControllerClass, "mStatusBarViewTouchEventHandler") != null) { //13 QPR1
			tryHookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Object mStatusBarViewTouchEventHandler = getObjectField(param.thisObject, "mStatusBarViewTouchEventHandler");
					tryHookAllMethods(mStatusBarViewTouchEventHandler.getClass(), "handleTouchEvent", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[0];
							if (event.getAction() == MotionEvent.ACTION_DOWN) {
								if (XPrefs.Xprefs.getBoolean("enableStatusBarVibration", false))
									SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
							}
						}
					});
				}
			});
		} else {
			if (hookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() { //13 QPR2
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					tryHookAllMethods(param.getResult().getClass(), "onTouch", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[1];
							if (event.getAction() == MotionEvent.ACTION_DOWN) {
								if (XPrefs.Xprefs.getBoolean("enableStatusBarVibration", false))
									SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
							}
						}
					});
				}
			}).isEmpty()) { //13 QPR3 - 14
				Class<?> PhoneStatusBarViewControllerClass = findClassIfExists("com.android.systemui.statusbar.phone.PhoneStatusBarViewController", lpparam.classLoader);
				XC_MethodHook statusbarTouchHook = new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						MotionEvent event = param.args[0] instanceof MotionEvent ? (MotionEvent) param.args[0] : (MotionEvent) param.args[1];
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							if (XPrefs.Xprefs.getBoolean("enableStatusBarVibration", false))
								SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
						}
					}
				};
				tryHookAllMethods(PhoneStatusBarViewControllerClass, "onTouch", statusbarTouchHook);
			}
		}
		if (oneFingerPulldownEnabled) {
			Class<?> QuickSettingsControllerImpl = findClassIfExists("com.android.systemui.shade.QuickSettingsControllerImpl", lpparam.classLoader);
			if (QuickSettingsControllerImpl != null) {
				tryHookAllMethods(QuickSettingsControllerImpl, "isOpenQsEvent", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						MotionEvent event = (MotionEvent) param.args[0];
						final int pointerCount = event.getPointerCount();
						final int action = event.getActionMasked();
						final boolean twoFingerDrag = action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2;
						final boolean stylusButtonClickDrag = action == MotionEvent.ACTION_DOWN && (event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY) || event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY));
						final boolean mouseButtonClickDrag = action == MotionEvent.ACTION_DOWN && (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY) || event.isButtonPressed(MotionEvent.BUTTON_TERTIARY));

						int w = mContext.getResources().getDisplayMetrics().widthPixels;
						float x = event.getX();
						float region = w * statusbarPortion;
						boolean quickPullApproved = (pullDownSide == PULLDOWN_SIDE_RIGHT) ? w - region < x : x < region;
						quickPullApproved &= getIntField(param.thisObject, "mBarState") == STATUSBAR_MODE_SHADE;

						param.setResult(twoFingerDrag || quickPullApproved || stylusButtonClickDrag || mouseButtonClickDrag);
					}
				});
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
	}
}