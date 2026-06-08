package sh.siava.AOSPMods.myListeners;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.content.res.XResources;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.Helper;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSQuickPullDown extends XposedModPack {
    public static final int PULLDOWN_SIDE_RIGHT = 1;
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    @SuppressWarnings("unused")
    private static final int PULLDOWN_SIDE_LEFT = 2;
    private static final int STATUSBAR_MODE_SHADE = 0;

    public static int pullDownSide = PULLDOWN_SIDE_RIGHT;
    private static boolean oneFingerPulldownEnabled = false;
    private static boolean enableStatusBarVibration = false;
    public static float statusbarPortion = 0.50f; // now set to 50% of the screen. it can be anything between 0 to 100%
    boolean enableLauncherQQS = false;

    boolean canVibrate = true;

    public QSQuickPullDown(Context context) {
        super(context);
    }

    public static void updatePrefs() {
        statusbarPortion = Xprefs.getInt("QSPulldownPercent", 50) / 100f;
        pullDownSide = Integer.parseInt(Xprefs.getString("QSPulldownSide", "1"));
    }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        oneFingerPulldownEnabled = Xprefs.getBoolean("QSPulldownEnabled", false);
        enableStatusBarVibration = Xprefs.getBoolean("enableStatusBarVibration", false);
        enableLauncherQQS = Xprefs.getBoolean("enableLauncherQQS", false);
        updatePrefs();
    }

    boolean quickPullApproved = false;

    public static boolean isQuickPullApproved(float x) {
        int w = Helper.INSTANCE.getWidthPixels();
        int widthToCheck;
        if (XResources.getSystem().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
            widthToCheck = w;
        } else {
            widthToCheck = Helper.INSTANCE.getHeightPixels();
        }
        float region = w * statusbarPortion;
        return (pullDownSide == PULLDOWN_SIDE_RIGHT) ? widthToCheck - region < x : x < region;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;
        if (oneFingerPulldownEnabled || enableLauncherQQS || enableStatusBarVibration) {
            if (oneFingerPulldownEnabled) {
                Class<?> NotificationPanelViewController$TouchHandler = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController$TouchHandler", lpparam.classLoader);
                if (NotificationPanelViewController$TouchHandler != null) {
                    tryHookAllMethods(NotificationPanelViewController$TouchHandler, "onInterceptTouchEvent", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            MotionEvent event = (MotionEvent) param.args[0];
                            if (event.getY() == 0) {
                                event.setLocation(event.getX(), 1);
                            }
                        }
                    });
                }
            }
            Class<?> QuickSettingsControllerImpl = findClassIfExists("com.android.systemui.shade.QuickSettingsControllerImpl", lpparam.classLoader);
            if (QuickSettingsControllerImpl != null) {
                tryHookAllMethods(QuickSettingsControllerImpl, "isOpenQsEvent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent event = (MotionEvent) param.args[0];
                        final int action = event.getActionMasked();
                        if (enableStatusBarVibration) {
                            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                                canVibrate = true;
                            }
                            float eventY = event.getY();
                            if (action == MotionEvent.ACTION_MOVE && eventY > 80 && eventY < 500 && canVibrate) {
                                canVibrate = false;
                                SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
                            }
                        }
                        if (oneFingerPulldownEnabled || enableLauncherQQS) {
                            if (action == MotionEvent.ACTION_DOWN) {
                                if (enableLauncherQQS) {
                                    callMethod(param.thisObject, "setStatusBarMinHeight", Helper.INSTANCE.getHeightPixels());
                                }
                                if (oneFingerPulldownEnabled) {
                                    quickPullApproved = isQuickPullApproved(event.getX());
                                    quickPullApproved &= getIntField(param.thisObject, "mBarState") == STATUSBAR_MODE_SHADE;
                                }
                            }
                            if (oneFingerPulldownEnabled) {
                                final int pointerCount = event.getPointerCount();
                                final boolean twoFingerDrag = action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2;
                                final boolean stylusButtonClickDrag = action == MotionEvent.ACTION_DOWN && (event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY) || event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY));
                                final boolean mouseButtonClickDrag = action == MotionEvent.ACTION_DOWN && (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY) || event.isButtonPressed(MotionEvent.BUTTON_TERTIARY));
                                param.setResult(twoFingerDrag || quickPullApproved || stylusButtonClickDrag || mouseButtonClickDrag);
                            }
                        }
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