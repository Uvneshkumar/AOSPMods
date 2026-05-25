package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.myListeners.QSQuickPullDown.PULLDOWN_SIDE_RIGHT;
import static sh.siava.AOSPMods.myListeners.SystemFrameworkListener.GO_TO_SLEEP_REASON_POWER_BUTTON;
import static sh.siava.AOSPMods.utils.Helpers.getFpRect;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.service.notification.StatusBarNotification;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.CustomDateAlarmLayout;
import sh.siava.AOSPMods.myListeners.helper.Helper;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class SystemUIListener extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    long pulseDelay = 1000;
    final Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable = null;

    final Handler handler2 = new Handler(Looper.getMainLooper());
    Runnable runnable2 = null;

    float previousY = 0;
    float previousDiff = 0;
    float previousAlpha = -1f;
    boolean shouldAnimate = false;

    boolean isAodIconVisible = true;
    boolean isTouchHandlingViewLongPressed = false;

    boolean isOneHandedModeActive = false;

    boolean hasSlept = false;
    ViewGroup ST2S_BP4A_KeyguardRootView = null;
    ViewGroup ST2S_BP4A_NotificationStackScrollLayout = null;
    String[] blockedViews = {
            "com.google.android.systemui.smartspace.DateSmartspaceView",
            "app:id/date_smartspace_view_large",
            "app:id/date_smartspace_view",
            "com.google.android.systemui.smartspace.BcSmartspaceView",
            "app:id/bc_smartspace_view",
//            "com.android.systemui.shared.clocks.view.FlexClockViewGroup",
//            "com.android.systemui.shared.clocks.view.FlexClockTextView",
            "com.android.keyguard.KeyguardSliceView",
            "app:id/keyguard_slice_view",
            "com.android.systemui.keyguard.ui.view.DeviceEntryIconView",
            "app:id/device_entry_icon_view",
            "com.android.systemui.statusbar.notification.stack.MediaContainerView",
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow",
            "app:id/expandableNotificationRow",
            "com.android.systemui.statusbar.notification.emptyshade.ui.view.EmptyShadeView"
    };

    private void initializeRunnable(Object thisObject) {
        handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            public void run() {
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/packages/SystemUI/src/com/android/systemui/doze/DozeLog.java;l=557;drc=70468495b83418eb4a406b91daed502c74709745#:~:text=556-,557,-558
                callMethod(thisObject, "requestPulse", 9, true, null);
                handler.postDelayed(this, pulseDelay);
            }
        };
    }

    private void initializeRunnable2(Object thisObject) {
        handler2.removeCallbacks(runnable2);
        runnable2 = new Runnable() {
            public void run() {
                callMethod(thisObject, "gentleWakeUp", 9);
            }
        };
    }

    public SystemUIListener(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
    }

    public final String CLIPBOARD_OVERLAY_SHOW_ACTIONS = "clipboard_overlay_show_actions";
    public final String NAMESPACE_SYSTEMUI = "systemui";

    private static final int SHADE = 0; // frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarState.java - screen unlocked - pulsing means screen is locked - shade locked means (Q)QS is open on lockscreen
    GestureDetector mDoubleTapToSleep; // event callback for double tap to sleep detection of statusbar only
    private Object NotificationPanelViewController;

    View touchHandlingView;
    Object touchHandlingViewListener;
    ImageView myIcon;

    int colorRed = Color.parseColor("#ffea4234");
    int colorYellow = Color.parseColor("#fffbbc06");
    int colorBlue = Color.parseColor("#ff4185f4");
    int colorGreen = Color.parseColor("#ff3aa853");
    int currentAssistantColourCount = 0;

    View mClearAllButton;

    float previousScrimAmount = -1f;
    float currentScrimAmount = -1f;
    float fingerprintX = 0f;
    float fingerprintY = 0f;
    boolean isLiftScrimAndSleepFromPowerButton = false;
    boolean shouldContinueScrim = true;
    boolean allLightRevealScrimFixBP4A = false;
    boolean onlyLiftRevealScrimFixBP4A = false;
    private final Handler overrideLastTapXYRemoveHandler = new Handler(Looper.getMainLooper());
    private final Runnable overrideLastTapXYRemoveRunnable = () -> {
        Xprefs.edit().putString("overrideLastTapXY", "").apply();
        Helper.INSTANCE.resetTapPosition();
        previousScrimAmount = -1;
        currentScrimAmount = -1;
        shouldContinueScrim = true;
        isLiftScrimAndSleepFromPowerButton = false;
    };

    private final Handler preventDarkStatusBarHandler = new Handler(Looper.getMainLooper());
    int LightBarTransitionsController_DEFAULT_TINT_ANIMATION_DURATION = 120;
    boolean shouldPreventDarkStatusBarAnimation = false;
    private final Runnable preventDarkStatusBarRunnable = () -> shouldPreventDarkStatusBarAnimation = false;

    private void adjustClockMargin(XC_MethodHook.MethodHookParam param) {
        TextView textView = (TextView) param.thisObject;
        if (!textView.isSingleLine()) {
            int largeClockTopMarginDynamic = Helper.INSTANCE.getPx((int) Float.parseFloat(Xprefs.getString("largeClockTopMarginDynamic", "80")));
            textView.setPadding(0, Math.min(0, largeClockTopMarginDynamic), 0, Math.max(0, largeClockTopMarginDynamic));
        }
    }

    private final View.OnLayoutChangeListener keyguardSliceViewLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
        LinearLayout linearLayout = (LinearLayout) v;
        if (linearLayout.getChildCount() > 1) {
            ViewGroup child = (ViewGroup) linearLayout.getChildAt(1);
            if (child.getChildCount() > 0) {
                for (int i = 0; i < child.getChildCount(); i++) {
                    View innerChild = child.getChildAt(i);
                    innerChild.setPadding(innerChild.getPaddingLeft(), innerChild.getPaddingTop(), Helper.INSTANCE.getPx(8), innerChild.getPaddingBottom());
                }
            }
        }
    };

    private boolean isOnLockScreen(String className) {
        return className.contains("KeyguardRootView") || className.contains("KeyguardStatusAreaView");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;
        int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
        int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
        int statusBarHeight;
        @SuppressLint("InternalInsetResource") int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        } else {
            statusBarHeight = 0;
        }
        if (Xprefs.getBoolean("disableLocationPrivacyIndicator", false)) {
            Class<?> PrivacyConfig = findClassIfExists("com.android.systemui.privacy.PrivacyConfig", lpparam.classLoader);
            if (PrivacyConfig != null) {
                tryHookAllMethods(PrivacyConfig, "isLocationEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
                tryHookAllConstructors(PrivacyConfig, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "locationAvailable", false);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disableLockScreenBounce", false)) {
            Class<?> NotificationPanelViewController = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
            if (NotificationPanelViewController != null) {
                tryHookAllMethods(NotificationPanelViewController, "onEmptySpaceClick", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (SystemUtils.KeyguardManager().isKeyguardLocked()) {
                            SystemUtils.Sleep();
                            param.setResult(null);
                        }
                    }
                });
            }
            // One UI
            Class<?> KeyguardTouchAnimator = findClassIfExists("com.android.systemui.keyguard.animator.KeyguardTouchAnimator", lpparam.classLoader);
            if (KeyguardTouchAnimator != null) {
                RectF fpRect = getFpRect();
                final float[] initialX = {-1};
                final float[] initialY = {-1};
                final long[] initialTime = {-1};
                int tapDiff = 50;
                tryHookAllMethods(KeyguardTouchAnimator, "onTouchEvent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent event = (MotionEvent) param.args[0];
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            initialX[0] = event.getX();
                            initialY[0] = event.getY();
                            initialTime[0] = System.currentTimeMillis();
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            float currentX = event.getX();
                            float currentY = event.getY();
                            long currentTime = System.currentTimeMillis();
                            if (!(fpRect.contains(initialX[0], initialY[0]))) {
                                if (Math.abs(currentX - initialX[0]) < tapDiff && Math.abs(currentY - initialY[0]) < tapDiff && Math.abs(currentTime - initialTime[0]) < 100) {
                                    param.setResult(true);
                                    SystemUtils.Sleep();
                                }
                            }
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disableLockScreenBounceBP4A", false)) {
            Class<?> KeyguardIndicationTextView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardIndicationTextView", lpparam.classLoader);
            if (KeyguardIndicationTextView != null) {
                tryHookAllMethods(KeyguardIndicationTextView, "switchIndication", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] instanceof String mMessage) {
                            if (mMessage != null && mMessage.contains("Swipe up to open") && !hasSlept) {
                                hasSlept = true;
                                SystemUtils.Sleep();
                            }
                        }
                    }
                });
            }
            Class<?> NotificationPanelViewController = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
            if (NotificationPanelViewController != null) {
                GestureDetector mTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(@NonNull MotionEvent e) {
//                        if allLightRevealScrimFixBP4A
//                        Helper.INSTANCE.setLastTapX(e.getX());
//                        Helper.INSTANCE.setLastTapY(e.getY());
                        hasSlept = false;
                        View currentViewInKeyguardRootView = findViewAt(ST2S_BP4A_KeyguardRootView, e.getX(), e.getY());
                        View currentViewInNotificationStackScrollLayout = findViewAt(ST2S_BP4A_NotificationStackScrollLayout, e.getX(), e.getY());
                        if (isAllowed(currentViewInKeyguardRootView) && isAllowed(currentViewInNotificationStackScrollLayout)) {
                            hasSlept = true;
                            SystemUtils.Sleep();
                        }
                        return super.onSingleTapUp(e);
                    }
                });
                tryHookAllMethods(NotificationPanelViewController, "handleExternalInterceptTouch", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (SystemUtils.KeyguardManager().isKeyguardLocked()) {
                            int mBarState = (int) getObjectField(param.thisObject, "mBarState");
                            if (mBarState != 2) {
                                mTapToSleep.onTouchEvent((MotionEvent) param.args[0]);
                            }
                        }
                    }
                });
            }
            Class<?> KeyguardRootView = findClassIfExists("com.android.systemui.keyguard.ui.view.KeyguardRootView", lpparam.classLoader);
            if (KeyguardRootView != null) {
                tryHookAllConstructors(KeyguardRootView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ST2S_BP4A_KeyguardRootView = (ViewGroup) param.thisObject;
                    }
                });
            }
            Class<?> NotificationStackScrollLayout = findClassIfExists("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", lpparam.classLoader);
            if (NotificationStackScrollLayout != null) {
                tryHookAllConstructors(NotificationStackScrollLayout, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ST2S_BP4A_NotificationStackScrollLayout = (ViewGroup) param.thisObject;
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disableLockScreenBounceAmbient", false)) {
            Class<?> AmbientIndicationContainer = findClassIfExists("com.google.android.systemui.ambientmusic.AmbientIndicationContainer", lpparam.classLoader);
            if (AmbientIndicationContainer != null) {
                tryHookAllConstructors(AmbientIndicationContainer, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.addOnLayoutChangeListener((view1, i, i1, i2, i3, i4, i5, i6, i7) -> {
                            view1.setVisibility(View.GONE);
                        });
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
        if (Xprefs.getBoolean("aodIconsCenter", false)) {
            Class<?> StatusBarIconView = findClassIfExists("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader);
            if (StatusBarIconView != null) {
                tryHookAllMethods(StatusBarIconView, "updateIconColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (isOnLockScreen(((ImageView) param.thisObject).getParent().getParent().toString())) {
                                param.setResult(null);
                                callMethod(param.thisObject, "setColorFilter", (Object) null);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
                tryHookAllMethods(StatusBarIconView, "updateDrawable", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (isOnLockScreen(((ImageView) param.thisObject).getParent().getParent().toString())) {
                                if (isAodIconVisible) {
                                    Helper.INSTANCE.setNotificationIcon((ImageView) param.thisObject, (StatusBarNotification) getObjectField(param.thisObject, "mNotification"), true, false);
                                } else {
                                    ((ImageView) param.thisObject).setImageDrawable(null);
                                }
                                param.setResult(true);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
            // One UI
            Class<?> LockscreenNotificationIconsOnlyController = findClassIfExists("com.android.systemui.statusbar.iconsOnly.LockscreenNotificationIconsOnlyController", lpparam.classLoader);
            if (LockscreenNotificationIconsOnlyController != null) {
                tryHookAllMethods(LockscreenNotificationIconsOnlyController, "onNotificationInfoUpdated", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup viewGroup = (ViewGroup) callMethod(param.thisObject, "getIconContainer");
                        ArrayList<?> paramArrayList = (ArrayList<?>) param.args[0];
                        if (viewGroup != null && viewGroup.getChildCount() > 0) {
                            ViewGroup notificationIconsOnlyContainer = (ViewGroup) viewGroup.getChildAt(0);
                            notificationIconsOnlyContainer.removeAllViews();
                            if (!paramArrayList.isEmpty()) {
                                notificationIconsOnlyContainer.setScaleX(1.5f);
                                notificationIconsOnlyContainer.setScaleY(1.5f);
                                for (int i = 0; i < paramArrayList.size(); i++) {
                                    notificationIconsOnlyContainer.addView(new ImageView(notificationIconsOnlyContainer.getContext()));
                                }
                                for (int i = 0; i < notificationIconsOnlyContainer.getChildCount(); i++) {
                                    Helper.INSTANCE.setNotificationIcon((ImageView) notificationIconsOnlyContainer.getChildAt(i), (StatusBarNotification) getObjectField(paramArrayList.get(i), "mSbn"), true, true);
                                }
                            }
                        }
                    }
                });
            }
        }
        int oneUiAodBrightness = Integer.parseInt(Xprefs.getString("oneUiAodBrightness", "-2"));
        if (oneUiAodBrightness > -2) {
            Class<?> AODMachine = findClassIfExists("com.android.systemui.doze.AODMachine", lpparam.classLoader);
            if (AODMachine != null) {
                tryHookAllMethods(AODMachine, "onUpdateDozeBrightness", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length >= 3) {
                            param.args[2] = oneUiAodBrightness;
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("delayFirstNotificationIconInAod", false)) {
            Class<?> NotificationIconContainer = findClassIfExists("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader);
            if (NotificationIconContainer != null) {
                tryHookAllMethods(NotificationIconContainer, "onViewAdded", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup viewGroup = (ViewGroup) param.thisObject;
                        if (isOnLockScreen(viewGroup.getParent().toString())) {
                            if (viewGroup.getChildCount() == 1) {
                                isAodIconVisible = false;
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    isAodIconVisible = true;
                                    Helper.INSTANCE.setNotificationIcon((ImageView) param.args[0], (StatusBarNotification) getObjectField(param.args[0], "mNotification"), true, false);
                                }, 1000);
                            }
                        }
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
        boolean whiteLockClock = Xprefs.getBoolean("whiteLockClock", false);
        boolean whiteLockClockAOD = Xprefs.getBoolean("whiteLockClockAOD", false);
        if (whiteLockClock || whiteLockClockAOD) {
            if (whiteLockClock) {
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
            Class<?> SimpleDigitalClockTextView = findClassIfExists("com.android.systemui.shared.clocks.view.SimpleDigitalClockTextView", lpparam.classLoader);
            if (SimpleDigitalClockTextView != null) {
                int aodColor = Color.WHITE;
//				float aodFontSizePx = 500; // Detect Large and Small and set separately
                String lsFontVariation = "'wght' 440, 'wdth' 100, 'ROND' 100, 'slnt' 0";
                String aodFontVariation = "'wght' 120, 'wdth' 100, 'ROND' 100, 'slnt' 0";
                tryHookAllMethods(SimpleDigitalClockTextView, "updateColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (whiteLockClock) {
                            param.args[0] = Color.WHITE;
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (whiteLockClockAOD) {
                            TextView view = (TextView) param.thisObject;
                            setObjectField(view, "aodColor", aodColor);
//							setObjectField(view, "aodFontSizePx", aodFontSizePx);
                            setObjectField(view, "lsFontVariation", lsFontVariation);
                            setObjectField(view, "aodFontVariation", aodFontVariation);
                            setObjectField(view, "fidgetFontVariation", lsFontVariation);
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("largeClockTopMargin", false)) {
            Class<?> AnimatableClockView = findClassIfExists("com.android.systemui.shared.clocks.AnimatableClockView", lpparam.classLoader);
            if (AnimatableClockView != null) {
                tryHookAllMethods(AnimatableClockView, "animateAppearOnLockscreen", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        adjustClockMargin(param);
                    }
                });
                tryHookAllMethods(AnimatableClockView, "animateFoldAppear", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        adjustClockMargin(param);
                    }
                });
                tryHookAllMethods(AnimatableClockView, "animateDoze", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        adjustClockMargin(param);
                    }
                });
            }
        }
        boolean largeClockTopMarginA16 = Xprefs.getBoolean("largeClockTopMarginA16", false);
        boolean largeClockDateSmartSpaceTopMarginA16 = Xprefs.getBoolean("largeClockDateSmartSpaceTopMarginA16", false);
        boolean hideClockDateSmartSpaceA16 = Xprefs.getBoolean("hideClockDateSmartSpaceA16", false);
        boolean fixHiddenLargeDateSmartSpaceA16 = Xprefs.getBoolean("fixHiddenLargeDateSmartSpaceA16", false);
        if (largeClockTopMarginA16 || largeClockDateSmartSpaceTopMarginA16 || hideClockDateSmartSpaceA16 || fixHiddenLargeDateSmartSpaceA16) {
            Class<?> KeyguardRootView = findClassIfExists("com.android.systemui.keyguard.ui.view.KeyguardRootView", lpparam.classLoader);
            if (KeyguardRootView != null) {
                tryHookAllConstructors(KeyguardRootView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup view = (ViewGroup) param.thisObject;
                        float largeClockTopMarginDynamic = Float.parseFloat(Xprefs.getString("largeClockTopMarginDynamic", "80"));
                        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                        viewTreeObserver.addOnPreDrawListener(() -> {
                            int childCount = view.getChildCount();
                            View burn_in_layer = null;
                            View flex_clock_view = null;
                            View date_smartspace_view_large = null;
                            View date_smartspace_view = null;
                            for (int i = 0; i < childCount; i++) {
                                if (view.getChildAt(i).toString().contains("app:id/burn_in_layer")) {
                                    burn_in_layer = view.getChildAt(i);
                                } else if (view.getChildAt(i).toString().contains("com.android.systemui.shared.clocks.view.FlexClockView")) {
                                    flex_clock_view = view.getChildAt(i);
                                } else if (view.getChildAt(i).toString().contains("app:id/date_smartspace_view_large")) {
                                    date_smartspace_view_large = view.getChildAt(i);
                                } else if (view.getChildAt(i).toString().contains("app:id/date_smartspace_view")) {
                                    date_smartspace_view = view.getChildAt(i);
                                }
                            }
                            if (burn_in_layer != null) {
                                if (largeClockTopMarginA16 && flex_clock_view != null) {
                                    flex_clock_view.setTranslationY(burn_in_layer.getTranslationY() - largeClockTopMarginDynamic);
                                }
                                if (fixHiddenLargeDateSmartSpaceA16) {
                                    if (date_smartspace_view_large instanceof ViewGroup dateSmartspace) {
                                        for (int i = 0; i < dateSmartspace.getChildCount(); i++) {
                                            View dateSmartspaceChildView = dateSmartspace.getChildAt(i);
                                            dateSmartspaceChildView.setMinimumHeight(128);
                                        }
                                    }
                                }
                                if (largeClockDateSmartSpaceTopMarginA16 && date_smartspace_view_large != null) {
                                    date_smartspace_view_large.setTranslationY(burn_in_layer.getTranslationY() - largeClockTopMarginDynamic);
                                }
                            }
                            if (hideClockDateSmartSpaceA16) {
                                if (date_smartspace_view_large != null) {
                                    date_smartspace_view_large.setScaleX(0);
                                    date_smartspace_view_large.setAlpha(0);
                                }
                                if (date_smartspace_view != null) {
                                    date_smartspace_view.setScaleX(0);
                                    date_smartspace_view.setAlpha(0);
                                }
                            }
                            return true;
                        });
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideLockScreenStatusBar", false)) {
            Class<?> KeyguardStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);
            if (KeyguardStatusBarView != null) {
                XC_MethodHook hideLockScreenStatusBarCallback = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View mSystemIconsContainer = (View) getObjectField(param.thisObject, "mSystemIconsContainer");
                        callMethod(mSystemIconsContainer, "setVisibility", View.INVISIBLE);
                    }
                };
                tryHookAllMethods(KeyguardStatusBarView, "loadDimens", hideLockScreenStatusBarCallback);
                tryHookAllMethods(KeyguardStatusBarView, "onLayout", hideLockScreenStatusBarCallback);
            }
        }
        boolean hideAODBatteryIconOneUI = Xprefs.getBoolean("hideAODBatteryIconOneUI", false);
        boolean oneUIBatteryPercentageInQS = Xprefs.getBoolean("oneUIBatteryPercentageInQS", false);
        if (hideAODBatteryIconOneUI || oneUIBatteryPercentageInQS) {
            Class<?> BatteryMeterView = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);
            if (BatteryMeterView != null) {
                if (hideAODBatteryIconOneUI) {
                    tryHookAllConstructors(BatteryMeterView, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            if (view.getId() == View.NO_ID) {
                                view.setScaleX(0);
                            }
                        }
                    });
                }
                if (oneUIBatteryPercentageInQS) {
                    tryHookAllMethods(BatteryMeterView, "updatePercentText", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            LinearLayout view = (LinearLayout) param.thisObject;
                            if (view.toString().contains("app:id/batteryRemainingIcon")) {
                                TextView batteryPercentage = view.findViewWithTag("aospModsBatteryPercent");
                                if (batteryPercentage == null) {
                                    batteryPercentage = new TextView(mContext);
                                    batteryPercentage.setTag("aospModsBatteryPercent");
                                    batteryPercentage.setIncludeFontPadding(false);
                                    batteryPercentage.setTextColor(Color.WHITE);
                                    view.addView(batteryPercentage);
                                }
                                int mLevel = (int) getObjectField(param.thisObject, "mLevel");
                                String percentText = " " + mLevel + "%";
                                batteryPercentage.setText(percentText);
                            }
                        }
                    });
                }
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
            Class<?> DeviceEntryIconView = findClassIfExists("com.android.systemui.keyguard.ui.view.DeviceEntryIconView", lpparam.classLoader);
            if (DeviceEntryIconView != null) {
                tryHookAllConstructors(DeviceEntryIconView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView iconView = (ImageView) getObjectField(param.thisObject, "iconView");
                        iconView.setAlpha(0f);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("nothingLockIcon", false)) {
            Class<?> DeviceEntryIconView = findClassIfExists("com.android.systemui.keyguard.ui.view.DeviceEntryIconView", lpparam.classLoader);
            if (DeviceEntryIconView != null) {
                tryHookAllConstructors(DeviceEntryIconView, new XC_MethodHook() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        FrameLayout rootView = (FrameLayout) param.thisObject;
                        ImageView rootFpIcon = (ImageView) rootView.getChildAt(0);
                        myIcon = new ImageView(mContext);
                        myIcon.setImageDrawable(Helper.INSTANCE.createOvalDrawable());
                        int iconPadding = 5;
                        myIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                        rootView.addView(myIcon);
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) myIcon.getLayoutParams();
                        lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
                        lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
                        lp.gravity = Gravity.CENTER;
                        myIcon.setLayoutParams(lp);
                        float nothingLockIconScale = Float.parseFloat(Xprefs.getString("nothingLockIconScale", "1"));
                        rootView.setScaleX(nothingLockIconScale);
                        rootView.setScaleY(nothingLockIconScale);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            rootFpIcon.setVisibility(View.INVISIBLE);
                            if (Xprefs.getBoolean("nothingLockIconLongPress", false)) {
                                String viewName;
                                if (Xprefs.getBoolean("nothingLockIconLongPressCrashFix", false)) {
                                    viewName = "longPressHandlingView";
                                } else {
                                    viewName = "touchHandlingView";
                                }
                                touchHandlingView = (View) getObjectField(param.thisObject, viewName);
                                touchHandlingViewListener = getObjectField(touchHandlingView, "listener");
                                tryHookAllMethods(touchHandlingViewListener.getClass(), "onLongPressDetected", new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
                                        if (isTouchHandlingViewLongPressed) {
                                            param1.setResult(null);
                                        } else {
                                            isTouchHandlingViewLongPressed = true;
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> isTouchHandlingViewLongPressed = false, 100);
                                        }
                                    }
                                });
                                myIcon.setOnTouchListener((view, motionEvent) -> {
                                    callMethod(touchHandlingViewListener, "onLongPressDetected", touchHandlingView, false);
                                    return false;
                                });
                            }
                        }, 1000);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("nothingFpIcon", false)) {
            Class<?> DeviceEntryIconView = findClassIfExists("com.android.systemui.keyguard.ui.view.DeviceEntryIconView", lpparam.classLoader);
            if (DeviceEntryIconView != null) {
                tryHookAllConstructors(DeviceEntryIconView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        FrameLayout rootView = (FrameLayout) param.thisObject;
                        ImageView myIcon = (ImageView) rootView.getChildAt(0);
                        myIcon.setImageDrawable(Helper.INSTANCE.createOvalDrawable());
                        int nothingFpIconPadding = (int) Float.parseFloat(Xprefs.getString("nothingFpIconPadding", "10"));
                        myIcon.setPadding(nothingFpIconPadding, nothingFpIconPadding, nothingFpIconPadding, nothingFpIconPadding);
                        ViewTreeObserver viewTreeObserver = myIcon.getViewTreeObserver();
                        viewTreeObserver.addOnDrawListener(() -> {
                            myIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                        });
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideExtendUnlockMessage", false)) {
            Class<?> KeyguardIndicationTextView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardIndicationTextView", lpparam.classLoader);
            if (KeyguardIndicationTextView != null) {
                tryHookAllMethods(KeyguardIndicationTextView, "switchIndication", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView textView = (TextView) param.thisObject;
                        Object mMessage = getObjectField(param.thisObject, "mMessage");
                        if (mMessage != null) {
                            if (mMessage.toString().contains("Kept unlocked by Extend Unlock")) {
                                textView.setScaleY(0f);
                            } else {
                                textView.setScaleY(1f);
                            }
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideAllUnlockMessage", false)) {
            Class<?> KeyguardIndicationTextView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardIndicationTextView", lpparam.classLoader);
            if (KeyguardIndicationTextView != null) {
                tryHookAllMethods(KeyguardIndicationTextView, "switchIndication", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView textView = (TextView) param.thisObject;
                        textView.setScaleY(0f);
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
                XC_MethodHook longVibrateCallback = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        SystemUtils.vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, null);
                    }
                };
                tryHookAllMethods(QSTileImplClass, "click", vibrateCallback);
                tryHookAllMethods(QSTileImplClass, "longClick", longVibrateCallback);
            }
        }
        if (Xprefs.getBoolean("hideBuildNumber", false)) {
            Class<?> DevelopmentSettingRepository = findClassIfExists("com.android.systemui.development.data.repository.DevelopmentSettingRepository", lpparam.classLoader);
            if (DevelopmentSettingRepository != null) {
                tryHookAllMethods(DevelopmentSettingRepository, "access$checkDevelopmentSettingEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("enableClipboardSmartActions", false)) {
            Class<?> DeviceConfigClass = findClassIfExists("android.provider.DeviceConfig", lpparam.classLoader);
            if (DeviceConfigClass != null) {
                tryHookAllMethods(DeviceConfigClass, "getBoolean", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].equals(NAMESPACE_SYSTEMUI) && param.args[1].equals(CLIPBOARD_OVERLAY_SHOW_ACTIONS)) {
                            param.setResult(true);
                        }
                    }
                });
            }
        }
        try {
            String mode = Xprefs.getBoolean("sysUiTuner", false) ? "enable" : "disable";
            com.topjohnwu.superuser.Shell.cmd("pm " + mode + " com.android.systemui/.tuner.TunerActivity").exec();
        } catch (Exception ignored) {
        }
        if (Xprefs.getBoolean("dt2sStatusBar", false)) {
            mDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    SystemUtils.Sleep();
                    return true;
                }
            });
            Class<?> NotificationPanelViewControllerClass = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
            if (NotificationPanelViewControllerClass != null) {
                tryHookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        NotificationPanelViewController = param.thisObject;
                    }
                });
                tryHookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        NotificationPanelViewController = param.thisObject;
                    }
                });
            }
            Class<?> PhoneStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
            if (PhoneStatusBarView != null) {
                tryHookAllMethods(PhoneStatusBarView, "onTouchEvent", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!(boolean) getObjectField(NotificationPanelViewController, "mPulsing") && !(boolean) getObjectField(NotificationPanelViewController, "mDozing") && (int) getObjectField(NotificationPanelViewController, "mBarState") == SHADE && (boolean) callMethod(NotificationPanelViewController, "isFullyCollapsed")) {
                            mDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[param.args.length - 1]);
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disallowDeepAOD", false)) {
            Class<?> DozeTriggers = findClassIfExists("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
            if (DozeTriggers != null) {
                tryHookAllConstructors(DozeTriggers, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        initializeRunnable(param.thisObject);
                        handler.postDelayed(runnable, pulseDelay);
                    }
                });
                tryHookAllMethods(DozeTriggers, "transitionTo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Objects.equals(param.args[1].toString(), "FINISH")) {
                            handler.removeCallbacks(runnable);
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disallowDeepAODBetter", false)) {
            XC_MethodHook noDozeHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int dozeState = (int) param.args[0];
                    if (dozeState == Display.STATE_DOZE || dozeState == Display.STATE_DOZE_SUSPEND) {
                        param.setResult(null);
                    }
                }
            };
            Class<?> DozeTriggers = findClassIfExists("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
            if (DozeTriggers != null) {
                tryHookAllMethods(DozeTriggers, "onScreenState", noDozeHook);
            }
            Class<?> DozeService = findClassIfExists("com.android.systemui.doze.DozeService", lpparam.classLoader);
            if (DozeService != null) {
                tryHookAllMethods(DozeService, "setDozeScreenState", noDozeHook);
            }
            Class<?> DozeScreenState = findClassIfExists("com.android.systemui.doze.DozeScreenState", lpparam.classLoader);
            if (DozeScreenState != null) {
                tryHookAllMethods(DozeScreenState, "applyScreenState", noDozeHook);
            }
        }
        boolean directUnlockOnTouchInFpRegion = Xprefs.getBoolean("directUnlockOnTouchInFpRegion", false);
        boolean directUnlockOnTouchIn1By3Region = Xprefs.getBoolean("directUnlockOnTouchIn1By3Region", false);
        boolean directUnlockOnTouchIn1By3RegionHideFP = Xprefs.getBoolean("directUnlockOnTouchIn1By3RegionHideFP", false);
        if (directUnlockOnTouchInFpRegion || directUnlockOnTouchIn1By3Region) {
            Class<?> PulsingGestureListener = findClassIfExists("com.android.systemui.shade.PulsingGestureListener", lpparam.classLoader);
            if (PulsingGestureListener != null) {
                RectF fpRect = getFpRect();
                float twoThirdScreenHeight = heightPixels * 0.67f;
                tryHookAllMethods(PulsingGestureListener, "onSingleTapUp", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ((boolean) param.getResult() && param.args[0] instanceof MotionEvent event) {
                            if (fpRect.contains(event.getX(), event.getY()) || (directUnlockOnTouchIn1By3Region && event.getY() > twoThirdScreenHeight)) {
                                myIcon.setVisibility(View.INVISIBLE);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        callMethod(touchHandlingViewListener, "onLongPressDetected", touchHandlingView, false);
                                        if (!directUnlockOnTouchIn1By3RegionHideFP) {
                                            myIcon.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }, 100);
                            }
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disallowDeepAOD1", false)) {
            Class<?> DozeTriggers = findClassIfExists("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
            if (DozeTriggers != null) {
                tryHookAllMethods(DozeTriggers, "transitionTo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Runtime.getRuntime().exec("su -c echo 1 > /sys/class/sec/tsp/input/enabled");
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disallowDeepAOD2", false)) {
            Class<?> DozeTriggers = findClassIfExists("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
            if (DozeTriggers != null) {
                tryHookAllConstructors(DozeTriggers, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        initializeRunnable2(param.thisObject);
                        handler2.postDelayed(runnable2, 350);
                    }
                });
            }
            Class<?> AnimatableClockView = findClassIfExists("com.android.systemui.shared.clocks.AnimatableClockView", lpparam.classLoader);
            if (AnimatableClockView != null) {
                tryHookAllConstructors(AnimatableClockView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "lockScreenWeightInternal", getObjectField(param.thisObject, "dozingWeightInternal"));
                    }
                });
            }
        }
        if (Xprefs.getBoolean("st2wOneUiAOD", false)) {
            Class<?> SecLightRevealScrimHelper = findClassIfExists("com.android.systemui.statusbar.SecLightRevealScrimHelper$start$broadcastReceiver$1", lpparam.classLoader);
            if (SecLightRevealScrimHelper != null) {
                tryHookAllMethods(SecLightRevealScrimHelper, "onReceive", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//						Intent paramIntent = (Intent) param.args[1];
                        callMethod(SystemUtils.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                    }
                });
            }
        }
        String pixelAODBrightnessFloat = Xprefs.getString("pixelAODBrightnessFloat", "");
        if (pixelAODBrightnessFloat != null && !pixelAODBrightnessFloat.isEmpty()) {
            Class<?> DozeService = findClassIfExists("com.android.systemui.doze.DozeService", lpparam.classLoader);
            if (DozeService != null) {
                float pixelAODBrightness = Float.parseFloat(pixelAODBrightnessFloat);
                XC_MethodHook setDozeScreenBrightness = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] instanceof Integer) {
                            param.args[0] = (int) (255 * pixelAODBrightness);
                        } else {
                            param.args[0] = pixelAODBrightness;
                        }
                    }
                };
                tryHookAllMethods(DozeService, "setDozeScreenBrightnessFloat", setDozeScreenBrightness);
                tryHookAllMethods(DozeService, "setDozeScreenBrightness", setDozeScreenBrightness);
            }
        }
        if (Xprefs.getBoolean("disableNewBackAffordance", false)) {
            Class<?> EdgeBackGestureHandler = findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
            if (EdgeBackGestureHandler != null) {
                tryHookAllMethods(EdgeBackGestureHandler, "resetEdgeBackPlugin", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        setBooleanField(param.thisObject, "mIsNewBackAffordanceEnabled", false);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideFpIcon", false)) {
            Class<?> BiometricPromptLayout = findClassIfExists("com.android.systemui.biometrics.ui.BiometricPromptLayout", lpparam.classLoader);
            if (BiometricPromptLayout != null) {
                tryHookAllMethods(BiometricPromptLayout, "onLayout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        LinearLayout biometricPromptLayout = (LinearLayout) param.thisObject;
                        int totalViews = Integer.parseInt(callMethod(param.thisObject, "getChildCount").toString());
                        for (int i = 0; i < totalViews; i++) {
                            if (biometricPromptLayout.getChildAt(i).toString().contains("biometric_icon_frame")) {
                                FrameLayout biometricIconFrame = (FrameLayout) biometricPromptLayout.getChildAt(i);
                                biometricIconFrame.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
            Class<?> BiometricViewBinder = findClassIfExists("com.android.systemui.biometrics.ui.binder.BiometricViewBinder", lpparam.classLoader);
            if (BiometricViewBinder != null) {
                tryHookAllMethods(BiometricViewBinder, "bind", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup rootView = (ViewGroup) param.args[0];
                        for (int i = 0; i < rootView.getChildCount(); i++) {
                            String currentView = rootView.getChildAt(i).toString();
                            if (currentView.contains("id/button_bar")) {
                                if (Xprefs.getBoolean("hideFpIconButtonBar", false)) {
                                    ViewGroup buttonBar = (ViewGroup) rootView.getChildAt(i);
                                    ViewGroup.LayoutParams layoutParams = buttonBar.getLayoutParams();
                                    layoutParams.height = -100;
                                    buttonBar.setLayoutParams(layoutParams);
                                }
                            } else if (currentView.contains("id/biometric_icon")) {
                                View biometricIcon = rootView.getChildAt(i);
                                biometricIcon.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("fpIconShifting", false)) {
            int pixelsToShift = Integer.parseInt(Xprefs.getString("fpIconShiftingTranslationY", "80"));
            Class<?> DeviceEntryIconView = findClassIfExists("com.android.systemui.keyguard.ui.view.DeviceEntryIconView", lpparam.classLoader);
            if (DeviceEntryIconView != null) {
                tryHookAllConstructors(DeviceEntryIconView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                        FrameLayout rootView = (FrameLayout) param.thisObject;
                        ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
                        viewTreeObserver.addOnDrawListener(() -> {
                            if (isLandscape) {
                                rootView.setTranslationX(pixelsToShift);
                            } else {
                                rootView.setTranslationY(pixelsToShift);
                            }
                        });
                    }
                });
            }
            if (Xprefs.getBoolean("fpIconShifting2", false)) {
                Class<?> BiometricViewBinder = findClassIfExists("com.android.systemui.biometrics.ui.binder.BiometricViewBinder", lpparam.classLoader);
                if (BiometricViewBinder != null) {
                    tryHookAllMethods(BiometricViewBinder, "bind", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                            ViewGroup rootView = (ViewGroup) param.args[0];
                            for (int i = 0; i < rootView.getChildCount(); i++) {
                                String currentView = rootView.getChildAt(i).toString();
                                if (currentView.contains("id/button_bar")) {
                                    ViewGroup buttonBar = (ViewGroup) rootView.getChildAt(i);
                                    if (!isLandscape) {
                                        buttonBar.setTranslationY(-(pixelsToShift));
                                    }
                                }
                            }
                            if (isLandscape) {
                                rootView.setTranslationX(pixelsToShift);
                            } else {
                                rootView.setTranslationY(pixelsToShift);
                            }
                        }
                    });
                }
                Class<?> AuthRippleView = findClassIfExists("com.android.systemui.biometrics.AuthRippleView", lpparam.classLoader);
                if (AuthRippleView != null) {
                    tryHookAllConstructors(AuthRippleView, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            view.setTranslationY(pixelsToShift);
                        }
                    });
                }
            }
        }
        if (Xprefs.getBoolean("hideFpDwellAnimation", false)) {
            Class<?> DwellRippleShader = findClassIfExists("com.android.systemui.biometrics.DwellRippleShader", lpparam.classLoader);
            if (DwellRippleShader != null) {
                tryHookAllMethods(DwellRippleShader, "setColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = 0;
                    }
                });
            }
            Class<?> AuthRippleView = findClassIfExists("com.android.systemui.biometrics.AuthRippleView", lpparam.classLoader);
            if (AuthRippleView != null) {
                tryHookAllConstructors(AuthRippleView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.setScaleX(0f);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("powerButtonRevealScrimBottom", false)) {
            Class<?> PowerButtonReveal = findClassIfExists("com.android.systemui.statusbar.PowerButtonReveal", lpparam.classLoader);
            float OFF_SCREEN_START_AMOUNT = 0.05f;
            float INCREASE_MULTIPLIER = 1.25f;
            if (PowerButtonReveal != null) {
                tryHookAllMethods(PowerButtonReveal, "setRevealAmountOnScrim", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//					float powerButtonY = (float) getObjectField(param.thisObject, "powerButtonY");
                        float amount = (float) param.args[0];
                        View scrim = (View) param.args[1];
                        float powerButtonY = (float) scrim.getWidth() / 2;
                        Interpolator FAST_OUT_SLOW_IN_REVERSE = new PathInterpolator(0.8f, 0f, 0.6f, 1f);
                        float interpolatedAmount = FAST_OUT_SLOW_IN_REVERSE.getInterpolation(amount);
                        float threshold = 0.5f;
                        float fadeAmount = Math.max(0f, interpolatedAmount - threshold) * (1f / (1f - threshold));
                        setObjectField(scrim, "revealGradientEndColorAlpha", 1f - fadeAmount);
                        setObjectField(scrim, "interpolatedRevealAmount", interpolatedAmount);
                        callMethod(scrim, "setRevealGradientBounds", (scrim.getWidth() - powerButtonY) - scrim.getWidth() * interpolatedAmount, scrim.getHeight() * (1f + OFF_SCREEN_START_AMOUNT) - scrim.getHeight() * INCREASE_MULTIPLIER * interpolatedAmount, (scrim.getWidth() - powerButtonY) + scrim.getWidth() * interpolatedAmount, scrim.getHeight() * (1f + OFF_SCREEN_START_AMOUNT) + scrim.getHeight() * INCREASE_MULTIPLIER * interpolatedAmount);
                    }
                });
            }
        }
        allLightRevealScrimFixBP4A = Xprefs.getBoolean("allLightRevealScrimFixBP4A", false);
        onlyLiftRevealScrimFixBP4A = Xprefs.getBoolean("onlyLiftRevealScrimFixBP4A", false);
        if (allLightRevealScrimFixBP4A || onlyLiftRevealScrimFixBP4A) {
            Class<?> DeviceEntryIconView = findClassIfExists("com.android.systemui.keyguard.ui.view.DeviceEntryIconView", lpparam.classLoader);
            if (DeviceEntryIconView != null) {
                tryHookAllConstructors(DeviceEntryIconView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            FrameLayout rootView = (FrameLayout) param.thisObject;
                            rootView.post(() -> {
                                int[] location = new int[2];
                                rootView.getLocationOnScreen(location);
                                int x = location[0];
                                int y = location[1];
                                fingerprintX = Math.round(x + ((rootView.getMeasuredWidth() * rootView.getScaleX()) / 2f));
                                fingerprintY = Math.round(y + ((rootView.getMeasuredHeight() * rootView.getScaleY()) / 2f));
                            });
                        }, 1000);
                    }
                });
            }
            Class<?> ScreenOffAnimationController = findClassIfExists("com.android.systemui.statusbar.phone.ScreenOffAnimationController", lpparam.classLoader);
            if (ScreenOffAnimationController != null) {
                tryHookAllMethods(ScreenOffAnimationController, "onStartedGoingToSleep", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object wakefulnessLifecycle = getObjectField(param.thisObject, "wakefulnessLifecycle");
                        int mLastSleepReason = (int) getObjectField(wakefulnessLifecycle, "mLastSleepReason");
                        if (!SystemUtils.KeyguardManager().isKeyguardLocked() && mLastSleepReason == GO_TO_SLEEP_REASON_POWER_BUTTON) {
                            Resources res = mContext.getResources();
                            float powerButtonY = res.getDimensionPixelSize(res.getIdentifier("physical_power_button_center_screen_location_y", "dimen", mContext.getPackageName()));
                            Xprefs.edit().putString("overrideLastTapXY", widthPixels * 1.05 + "," + powerButtonY).apply();
                            overrideLastTapXYRemoveHandler.removeCallbacks(overrideLastTapXYRemoveRunnable);
                            overrideLastTapXYRemoveHandler.postDelayed(overrideLastTapXYRemoveRunnable, 1000);
                        }
                    }
                });
            }
            Class<?> LiftReveal = findClassIfExists("com.android.systemui.statusbar.LiftReveal", lpparam.classLoader);
            tryHookAllMethods(LiftReveal, "setRevealAmountOnScrim", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    circleReveal(param, widthPixels, heightPixels, RevealType.LIFT);
                }
            });
            if (allLightRevealScrimFixBP4A) {
                Class<?> PowerButtonReveal = findClassIfExists("com.android.systemui.statusbar.PowerButtonReveal", lpparam.classLoader);
                tryHookAllMethods(PowerButtonReveal, "setRevealAmountOnScrim", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        circleReveal(param, widthPixels, heightPixels, RevealType.POWER);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideKeyguardSliceView", false)) {
            Class<?> KeyguardSliceView = findClassIfExists("com.android.keyguard.KeyguardSliceView", lpparam.classLoader);
            if (KeyguardSliceView != null) {
                tryHookAllConstructors(KeyguardSliceView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ((View) param.thisObject).setVisibility(View.GONE);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("keyguardSliceViewLineagePadding", false)) {
            Class<?> KeyguardSliceView = findClassIfExists("com.android.keyguard.KeyguardSliceView", lpparam.classLoader);
            if (KeyguardSliceView != null) {
                tryHookAllConstructors(KeyguardSliceView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        LinearLayout keyguardSliceView = (LinearLayout) param.thisObject;
                        keyguardSliceView.setPadding(Helper.INSTANCE.getPx(3) + 3, keyguardSliceView.getPaddingTop(), keyguardSliceView.getPaddingRight(), keyguardSliceView.getPaddingBottom());
                    }
                });
            }
        }
        if (Xprefs.getBoolean("bcSmartspaceViewPadding", false)) {
            Class<?> BcSmartspaceView = findClassIfExists("com.google.android.systemui.smartspace.BcSmartspaceView", lpparam.classLoader);
            if (BcSmartspaceView != null) {
                tryHookAllConstructors(BcSmartspaceView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        FrameLayout bcSmartspaceView = (FrameLayout) param.thisObject;
                        bcSmartspaceView.setPaddingRelative(Helper.INSTANCE.getBcSmartspaceViewPadding(), 0, Helper.INSTANCE.getBcSmartspaceViewPadding(), 0);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("keyguardSliceViewAlarmPadding", false)) {
            Class<?> KeyguardSliceView = findClassIfExists("com.android.keyguard.KeyguardSliceView", lpparam.classLoader);
            if (KeyguardSliceView != null) {
                tryHookAllConstructors(KeyguardSliceView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        LinearLayout keyguardSliceView = (LinearLayout) param.thisObject;
                        keyguardSliceView.removeOnLayoutChangeListener(keyguardSliceViewLayoutChangeListener);
                        keyguardSliceView.addOnLayoutChangeListener(keyguardSliceViewLayoutChangeListener);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("keyguardSliceViewCustomA16", false)) {
            Class<?> KeyguardSliceView = findClassIfExists("com.android.keyguard.KeyguardSliceView", lpparam.classLoader);
            if (KeyguardSliceView != null) {
                tryHookAllMethods(KeyguardSliceView, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup viewGroup = (ViewGroup) param.thisObject;
                        viewGroup.removeAllViews();
                        viewGroup.addView(new CustomDateAlarmLayout(mContext));
                    }
                });
            }
        }
        if (Xprefs.getBoolean("keyguardSliceViewBurnInLikeSmartSpace", false)) {
            Class<?> KeyguardRootView = findClassIfExists("com.android.systemui.keyguard.ui.view.KeyguardRootView", lpparam.classLoader);
            if (KeyguardRootView != null) {
                tryHookAllConstructors(KeyguardRootView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup keyguardRootView = (ViewGroup) param.thisObject;
                        int bc_smartspace_view_Id = mContext
                                .getResources()
                                .getIdentifier("bc_smartspace_view", "id", "com.android.systemui");
                        int keyguard_slice_view_Id = mContext
                                .getResources()
                                .getIdentifier("keyguard_slice_view", "id", "com.android.systemui");
                        int burn_in_layer_Id = mContext
                                .getResources()
                                .getIdentifier("burn_in_layer", "id", "com.android.systemui");
                        View bc_smartspace_view = new View(mContext);
                        bc_smartspace_view.setId(bc_smartspace_view_Id);
                        final View[] keyguard_slice_view = {keyguardRootView.findViewById(keyguard_slice_view_Id)};
                        final View[] burn_in_layer = {keyguardRootView.findViewById(burn_in_layer_Id)};
                        final float[] lastY = {0};
                        final float[] lastTranslationXY = {0, 0};
                        keyguardRootView.addView(bc_smartspace_view);
                        ViewTreeObserver viewTreeObserver = keyguardRootView.getViewTreeObserver();
                        viewTreeObserver.addOnPreDrawListener(() -> {
                            if (keyguard_slice_view[0] == null) {
                                keyguard_slice_view[0] = keyguardRootView.findViewById(keyguard_slice_view_Id);
                            } else if (burn_in_layer[0] == null) {
                                burn_in_layer[0] = keyguardRootView.findViewById(burn_in_layer_Id);
                            } else {
                                if (lastY[0] != bc_smartspace_view.getY()
                                        || lastTranslationXY[0] != burn_in_layer[0].getTranslationX()
                                        || lastTranslationXY[1] != burn_in_layer[0].getTranslationY()) {
                                    keyguard_slice_view[0].setVisibility(burn_in_layer[0].getVisibility());
                                    keyguard_slice_view[0].setAlpha(burn_in_layer[0].getAlpha());
                                    if (Math.abs(lastY[0] - bc_smartspace_view.getY()) > 100 && keyguardRootView.getAlpha() >= 0.9) {
                                        Helper.INSTANCE.animateAlpha(keyguardRootView, 600);
                                    }
                                    lastY[0] = bc_smartspace_view.getY();
                                    lastTranslationXY[0] = burn_in_layer[0].getTranslationX();
                                    lastTranslationXY[1] = burn_in_layer[0].getTranslationY();
                                    keyguard_slice_view[0].setTranslationX(lastTranslationXY[0]);
                                    keyguard_slice_view[0].setY(lastY[0] + lastTranslationXY[1]);
                                }
                            }
                            return true;
                        });
                    }
                });
            }
        }
        if (Xprefs.getBoolean("keyguardSliceViewBurnIn", false)) {
            Class<?> KeyguardRootViewBinder = findClassIfExists("com.android.systemui.keyguard.ui.binder.KeyguardRootViewBinder", lpparam.classLoader);
            if (KeyguardRootViewBinder != null) {
                tryHookAllMethods(KeyguardRootViewBinder, "bind", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup view = (ViewGroup) param.args[0];
                        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                        viewTreeObserver.addOnPreDrawListener(() -> {
                            int childCount = view.getChildCount();
                            View burn_in_layer = null;
                            View keyguard_slice_view = null;
                            ViewGroup aod_notification_icon_container = null;
                            View lockscreen_clock_view_large = null;
                            for (int i = 0; i < childCount; i++) {
                                if (view.getChildAt(i).toString().contains("app:id/burn_in_layer")) {
                                    burn_in_layer = view.getChildAt(i);
                                }
                                if (view.getChildAt(i).toString().contains("app:id/keyguard_slice_view")) {
                                    keyguard_slice_view = view.getChildAt(i);
                                }
                                if (view.getChildAt(i).toString().contains("app:id/aod_notification_icon_container")) {
                                    aod_notification_icon_container = (ViewGroup) view.getChildAt(i);
                                }
                                if (view.getChildAt(i).toString().contains("app:id/lockscreen_clock_view_large") || view.getChildAt(i).toString().contains("com.android.systemui.shared.clocks.view.FlexClockView")) {
                                    lockscreen_clock_view_large = view.getChildAt(i);
                                }
                            }
                            if (burn_in_layer != null && keyguard_slice_view != null && aod_notification_icon_container != null && lockscreen_clock_view_large != null) {
                                float currentY = keyguard_slice_view.getY();
                                float currentDiff = Math.abs(previousY - currentY);
                                float currentAlpha = view.getAlpha();
                                if (previousY != currentY && previousDiff != currentDiff && currentDiff > 100 && currentAlpha >= 0.9) {
                                    // Clock Switch When Notification
                                    Helper.INSTANCE.animateAlpha(view);
                                }
                                previousY = currentY;
                                previousDiff = currentDiff;
                                if (currentAlpha != previousAlpha) {
                                    if (currentAlpha == 0) {
                                        if (Xprefs.getBoolean("keyguardSliceViewBurnIn2", false)) {
                                            shouldAnimate = true;
                                        }
                                        if (shouldAnimate) {
                                            view.setScaleX(0);
                                            view.setScaleY(0);
                                        }
                                    }
                                    // Check and Apply Anim when Entering Lock Screen AOD
                                    if (previousAlpha == 0 && !shouldAnimate) {
                                        boolean isLargeClockVisible = lockscreen_clock_view_large.getVisibility() == View.VISIBLE;
                                        boolean shouldSmallClockBeVisible = aod_notification_icon_container.getChildCount() != 0;
                                        shouldAnimate = shouldSmallClockBeVisible == isLargeClockVisible;
                                        if (shouldAnimate) {
                                            view.setScaleX(0);
                                            view.setScaleY(0);
                                        }
                                    }
                                    if (currentAlpha == 1 && shouldAnimate) {
                                        view.setScaleX(1);
                                        view.setScaleY(1);
                                        // Enter Lock Screen AOD from Home Screen
                                        Helper.INSTANCE.animateAppear(view);
                                        shouldAnimate = false;
                                    }
                                }
                                previousAlpha = currentAlpha;
                                keyguard_slice_view.setVisibility(burn_in_layer.getVisibility());
                                keyguard_slice_view.setAlpha(burn_in_layer.getAlpha());
                                keyguard_slice_view.setTranslationX(burn_in_layer.getTranslationX());
                                keyguard_slice_view.setTranslationY(burn_in_layer.getTranslationY());
                            }
                            return true;
                        });
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disableBurnIn", false)) {
            Class<?> KeyguardRootViewBinder = findClassIfExists("com.android.systemui.keyguard.ui.binder.KeyguardRootViewBinder", lpparam.classLoader);
            if (KeyguardRootViewBinder != null) {
                tryHookAllMethods(KeyguardRootViewBinder, "bind", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup view = (ViewGroup) param.args[0];
                        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                        viewTreeObserver.addOnPreDrawListener(() -> {
                            int childCount = view.getChildCount();
                            for (int i = 0; i < childCount; i++) {
                                view.getChildAt(i).setTranslationX(0);
                                view.getChildAt(i).setTranslationY(0);
                            }
                            return true;
                        });
                    }
                });
            }
        }
        if (Xprefs.getBoolean("fixStupidMissingNotificationBackground", false)) {
            Class<?> NotificationBackgroundView = findClassIfExists("com.android.systemui.statusbar.notification.row.NotificationBackgroundView", lpparam.classLoader);
            tryHookAllMethods(NotificationBackgroundView, "setTint", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((int) param.args[0] == 0) {
                        param.args[0] = getObjectField(param.thisObject, "mNormalColor");
                    }
                }
            });
        }
        if (Xprefs.getBoolean("fixStupidDarkStatusBarDelay", false)) {
            Class<?> LightBarControllerImpl = findClassIfExists("com.android.systemui.statusbar.phone.LightBarControllerImpl", lpparam.classLoader);
            if (LightBarControllerImpl != null) {
                tryHookAllMethods(LightBarControllerImpl, "onNavigationBarAppearanceChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mStatusBarIconController = getObjectField(param.thisObject, "mStatusBarIconController");
                        if (mStatusBarIconController != null) {
                            int appearance = (int) param.args[0];
                            float mDarkIntensity = (float) getObjectField(mStatusBarIconController, "mDarkIntensity");
                            if ((appearance == 0 || appearance == 16) && mDarkIntensity == 1) {
                                // 16, 1
                                // 0, 1
                                shouldPreventDarkStatusBarAnimation = true;
                                callMethod(mStatusBarIconController, "applyDarkIntensity", 0f);
                            } else if (appearance == 8 && mDarkIntensity == 0) {
                                // 8, 0
                                shouldPreventDarkStatusBarAnimation = true;
                                callMethod(mStatusBarIconController, "applyDarkIntensity", 1f);
                            }
                            if (shouldPreventDarkStatusBarAnimation) {
                                preventDarkStatusBarHandler.removeCallbacks(preventDarkStatusBarRunnable);
                                preventDarkStatusBarHandler.postDelayed(preventDarkStatusBarRunnable, LightBarTransitionsController_DEFAULT_TINT_ANIMATION_DURATION * 10L);
                            }
                        }
                    }
                });
            }
            Class<?> DarkIconDispatcherImpl = findClassIfExists("com.android.systemui.statusbar.phone.DarkIconDispatcherImpl", lpparam.classLoader);
            if (DarkIconDispatcherImpl != null) {
                tryHookAllMethods(DarkIconDispatcherImpl, "getTintAnimationDuration", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (shouldPreventDarkStatusBarAnimation) {
                            param.setResult(0);
                        } else {
                            param.setResult(LightBarTransitionsController_DEFAULT_TINT_ANIMATION_DURATION);
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("expandFirstNotification", false)) {
            Class<?> ExpandableNotificationRow = findClassIfExists("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", lpparam.classLoader);
            if (ExpandableNotificationRow != null) {
                tryHookAllMethods(ExpandableNotificationRow, "isExpanded", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        ViewGroup viewGroup = (ViewGroup) view.getParent();
                        if (view != null && viewGroup != null && viewGroup.indexOfChild(view) == 0 && !getBooleanField(param.thisObject, "mOnKeyguard")) {
                            param.setResult(true);
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("oxygenOsSystemUiFixes", false)) {
            Class<?> AodRootLayout = findClassIfExists("com.oplus.systemui.aod.aodclock.off.AodRootLayout", lpparam.classLoader);
            if (AodRootLayout != null) {
                tryHookAllMethods(AodRootLayout, "onTouchEvent", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent event = (MotionEvent) param.args[0];
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            callMethod(SystemUtils.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                        }
                    }
                });
            }
            Class<?> KeyguardFpUnlockHelper = findClassIfExists("com.oplus.systemui.biometrics.finger.KeyguardFpUnlockHelper", lpparam.classLoader);
            if (KeyguardFpUnlockHelper != null) {
                tryHookAllMethods(KeyguardFpUnlockHelper, "notifyShowFp", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        callMethod(SystemUtils.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                    }
                });
            }
            Class<?> OplusKeyguardBottomAreaController = findClassIfExists("com.oplus.systemui.keyguard.OplusKeyguardBottomAreaController", lpparam.classLoader);
            if (OplusKeyguardBottomAreaController != null) {
                tryHookAllMethods(OplusKeyguardBottomAreaController, "showKeyguardGlideTipImmediate", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        callMethod(SystemUtils.PowerManager(), "goToSleep", SystemClock.uptimeMillis());
                    }
                });
            }
            Class<?> FingerprintTipsController = findClassIfExists("com.oplus.systemui.keyguard.tips.FingerprintTipsController", lpparam.classLoader);
            if (FingerprintTipsController != null) {
                tryHookAllMethods(FingerprintTipsController, "displayKeyguardTips", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
            Class<?> OneHandedBackgroundPanelEx = findClassIfExists("com.oplus.onehanded.OneHandedBackgroundPanelEx", lpparam.classLoader);
            if (OneHandedBackgroundPanelEx != null) {
                tryHookAllMethods(OneHandedBackgroundPanelEx, "setCustomBackgroundDrawable", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
            Class<?> BackgroundWindowManager = findClassIfExists("com.android.wm.shell.onehanded.BackgroundWindowManager", lpparam.classLoader);
            if (BackgroundWindowManager != null) {
                tryHookAllMethods(BackgroundWindowManager, "showBackgroundLayer", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(BackgroundWindowManager, "removeBackgroundLayer", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
            // SideGestureNavView
            // SideGestureViewManager
            // OplusNavigationHandle
            // SideGestureDetector
        }
        if (Xprefs.getBoolean("assistantAnimationColors", false)) {
            Class<?> EdgeLight = findClassIfExists("com.android.systemui.assist.ui.EdgeLight", lpparam.classLoader);
            if (EdgeLight != null) {
                tryHookAllMethods(EdgeLight, "setEndpoints", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (currentAssistantColourCount < 4) {
                            if (currentAssistantColourCount == 0) {
                                setObjectField(param.thisObject, "mColor", colorBlue);
                            } else if (currentAssistantColourCount == 1) {
                                setObjectField(param.thisObject, "mColor", colorRed);
                            } else if (currentAssistantColourCount == 2) {
                                setObjectField(param.thisObject, "mColor", colorYellow);
                            } else {
                                setObjectField(param.thisObject, "mColor", colorGreen);
                            }
                            currentAssistantColourCount++;
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("fingerprintNotificationGestures", false)) {
            Class<?> FooterView = findClassIfExists("com.android.systemui.statusbar.notification.footer.ui.view.FooterView", lpparam.classLoader);
            if (FooterView != null) {
                tryHookAllMethods(FooterView, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mClearAllButton = (View) getObjectField(param.thisObject, "mClearAllButton");
                    }
                });
            }
            Class<?> CentralSurfacesCommandQueueCallbacks = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesCommandQueueCallbacks", lpparam.classLoader);
            if (CentralSurfacesCommandQueueCallbacks != null) {
                tryHookAllMethods(CentralSurfacesCommandQueueCallbacks, "handleSystemKey", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        KeyEvent keyEvent = (KeyEvent) param.args[0];
                        Object mPanelExpansionInteractor = getObjectField(param.thisObject, "mPanelExpansionInteractor");
                        boolean isFullyCollapsed = (boolean) callMethod(mPanelExpansionInteractor, "isFullyCollapsed");
                        Object mQsController = getObjectField(param.thisObject, "mQsController");
                        boolean getExpanded = (boolean) callMethod(mQsController, "getExpanded");
                        try {
                            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP) {
                                if (!isFullyCollapsed) {
                                    Shell.cmd("cmd statusbar collapse").exec();
                                }
                            } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN) {
                                if (isFullyCollapsed) {
                                    Shell.cmd("cmd statusbar expand-notifications").exec();
                                } else if (!getExpanded) {
                                    Shell.cmd("input swipe 250 400 250 800 30").submit();
                                }
                            } else {
                                if (mClearAllButton != null) {
                                    if (!isFullyCollapsed && !getExpanded) {
                                        if (mClearAllButton.hasOnClickListeners() && mClearAllButton.getVisibility() == View.VISIBLE) {
                                            mClearAllButton.performClick();
                                        } else {
                                            Shell.cmd("cmd statusbar collapse").exec();
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                        param.setResult(null);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("boldClockAndDateInStatusBar", false)) {
            Class<?> Clock = findClassIfExists("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
            if (Clock != null) {
                tryHookAllMethods(Clock, "updateClock$1", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView clockView = (TextView) param.thisObject;
                        clockView.setTypeface(XPrefs.modRes.getFont(R.font.google_sans_clock), Typeface.BOLD);
                    }
                });
            }
            Class<?> VariableDateView = findClassIfExists("com.android.systemui.statusbar.policy.VariableDateView", lpparam.classLoader);
            if (VariableDateView != null) {
                tryHookAllConstructors(VariableDateView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView dateView = (TextView) param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            dateView.setTypeface(XPrefs.modRes.getFont(R.font.google_sans_flex));
                            dateView.setFontVariationSettings("'wght' 600, 'ROND' 100");
                        }, 1000);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("allowOpeningQsInOneHandedBP4A", false)) {
            Class<?> OneHandedTouchHandler = findClassIfExists("com.android.wm.shell.onehanded.OneHandedTouchHandler", lpparam.classLoader);
            if (OneHandedTouchHandler != null) {
                tryHookAllMethods(OneHandedTouchHandler, "onStartFinished", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        isOneHandedModeActive = true;
                    }
                });
                tryHookAllMethods(OneHandedTouchHandler, "onStopFinished", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        isOneHandedModeActive = false;
                    }
                });
            }
            boolean oneFingerPulldownEnabled = Xprefs.getBoolean("QSPulldownEnabled", false);
            float statusbarPortion = Xprefs.getInt("QSPulldownPercent", 50) / 100f;
            int pullDownSide = Integer.parseInt(Xprefs.getString("QSPulldownSide", "1"));
            Class<?> QuickSettingsControllerImpl = findClassIfExists("com.android.systemui.shade.QuickSettingsControllerImpl", lpparam.classLoader);
            if (QuickSettingsControllerImpl != null) {
                tryHookAllMethods(QuickSettingsControllerImpl, "shouldQuickSettingsIntercept", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        float x = (float) param.args[0];
                        float y = (float) param.args[1];
                        if (isOneHandedModeActive && y <= statusBarHeight) {
                            boolean quickPullApproved = false;
                            if (oneFingerPulldownEnabled) {
                                float region = widthPixels * statusbarPortion;
                                quickPullApproved = (pullDownSide == PULLDOWN_SIDE_RIGHT) ? widthPixels - region < x : x < region;
                            }
                            if (quickPullApproved) {
                                Shell.cmd("cmd statusbar expand-settings").submit();
                            } else {
                                Shell.cmd("cmd statusbar expand-notifications").submit();
                            }
                        }
                    }
                });
            }
        }

//		Class<?> BackPanel = findClassIfExists("com.android.systemui.navigationbar.gestural.BackPanel", lpparam.classLoader);
//        if (BackPanel != null) {
//            tryHookAllConstructors(BackPanel, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//					new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                        Paint arrowBackgroundPaint = (Paint) getObjectField(param.thisObject, "arrowBackgroundPaint");
//                        arrowBackgroundPaint.setARGB(100, 100, 100, 100);
//						View view = (View) param.thisObject;
//						view.setAlpha(0.5f);
//                    }, 5000);
//                }
//            });
//        }
    }

    public View findViewAt(ViewGroup parent, float x, float y) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) { // topmost first
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            float childX = child.getX();
            float childY = child.getY();
            if (x >= childX && x <= childX + child.getWidth() &&
                    y >= childY && y <= childY + child.getHeight()) {
                return child;
            }
        }
        return null;
    }

    private boolean isAllowed(View view) {
        if (view == null) return true;
        String viewStr = view.toString();
        for (String blocked : blockedViews) {
            if (viewStr.contains(blocked)) {
                return false;
            }
        }
        return true;
    }

    private void circleReveal(XC_MethodHook.MethodHookParam param, int widthPixels, int heightPixels, RevealType revealType) {
        float amount = (float) param.args[0];
        int centerX = 0;
        int centerY = 0;
        if (revealType == RevealType.POWER) {
            float powerButtonY = (float) getObjectField(param.thisObject, "powerButtonY");
            centerX = (int) (widthPixels * 1.05);
            centerY = (int) powerButtonY;
        } else if (revealType == RevealType.LIFT) {
            if (currentScrimAmount == -1) {
                if (amount < 0.5) {
                    // Wake
                    // Always Wake from FP during Lift or some rare time when ST2W from AOD
                    Helper.INSTANCE.setLastTapX(fingerprintX);
                    Helper.INSTANCE.setLastTapY(fingerprintY);
                } else {
                    // Sleep
                    String[] overrideLastTapXY = Xprefs.getString("overrideLastTapXY", "").split(",");
                    if (overrideLastTapXY.length > 1) {
                        isLiftScrimAndSleepFromPowerButton = !allLightRevealScrimFixBP4A && onlyLiftRevealScrimFixBP4A;
                        float overrideLastTapX = Float.parseFloat(overrideLastTapXY[0].trim());
                        float overrideLastTapY = Float.parseFloat(overrideLastTapXY[1].trim());
                        Helper.INSTANCE.setLastTapX(overrideLastTapX);
                        Helper.INSTANCE.setLastTapY(overrideLastTapY);
                    } else if (onlyLiftRevealScrimFixBP4A) {
                        shouldContinueScrim = false;
                    }
                }
            }
            previousScrimAmount = currentScrimAmount;
            currentScrimAmount = amount;
            centerX = (int) Helper.INSTANCE.getLastTapX();
            centerY = (int) Helper.INSTANCE.getLastTapY();
        }
        if (shouldContinueScrim) {
            View scrim = (View) param.args[1];
            Interpolator interpolator;
            if (isLiftScrimAndSleepFromPowerButton) {
                // FAST_OUT_SLOW_IN_REVERSE
                interpolator = new PathInterpolator(0.8f, 0f, 0.6f, 1f);
            } else {
                // LEGACY (fastRevealInterpolator) - https://cs.android.com/android/platform/superproject/+/android16-qpr2-release:frameworks/libs/systemui/animationlib/src/com/android/app/animation/Interpolators.java
                interpolator = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
            }
            float interpolatedAmount = interpolator.getInterpolation(amount);
            float threshold = 0.5f;
            float fadeAmount = Math.max(0f, interpolatedAmount - threshold) * (1f / (1f - threshold));
            setObjectField(scrim, "revealGradientEndColorAlpha", 1f - fadeAmount);
            if (isLiftScrimAndSleepFromPowerButton) {
                float OFF_SCREEN_START_AMOUNT = 0.05f;
                float INCREASE_MULTIPLIER = 1.25f;
                callMethod(scrim, "setRevealGradientBounds", scrim.getWidth() * (1f + OFF_SCREEN_START_AMOUNT) -
                                scrim.getWidth() * INCREASE_MULTIPLIER * interpolatedAmount,
                        centerY - scrim.getHeight() * interpolatedAmount,
                        scrim.getWidth() * (1f + OFF_SCREEN_START_AMOUNT) +
                                scrim.getWidth() * INCREASE_MULTIPLIER * interpolatedAmount,
                        centerY + scrim.getHeight() * interpolatedAmount);
            } else {
                int startRadius = 0;
                int endRadius = Math.max(Math.max(centerX, widthPixels - centerX), Math.max(centerY, heightPixels - centerY));
                float radius = startRadius + ((endRadius - startRadius) * amount);
                callMethod(scrim, "setRevealGradientBounds", centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            }
        }
        overrideLastTapXYRemoveHandler.removeCallbacks(overrideLastTapXYRemoveRunnable);
        overrideLastTapXYRemoveHandler.postDelayed(overrideLastTapXYRemoveRunnable, 100);
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
    }

    enum RevealType {
        LIFT, POWER
    }
}
