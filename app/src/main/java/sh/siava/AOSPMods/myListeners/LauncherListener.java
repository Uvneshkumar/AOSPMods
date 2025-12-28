package sh.siava.AOSPMods.myListeners;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static com.topjohnwu.superuser.Shell.cmd;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.Helper;
import sh.siava.AOSPMods.myListeners.helper.MyBroadcastReceiver;
import sh.siava.AOSPMods.myListeners.helper.ScreenReceiver;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class LauncherListener extends XposedModPack {

    final boolean[] hasVibrated = {false};

    public LauncherListener(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
    }

    @Override
    public boolean listensTo(String packageName) {
        return true;
    }

    private Boolean canLock = false;

    private boolean isHomeTriggered = false;
    private Object launcherWorkspaceObject = null;
    private Object mLauncherOneUi = null;

    private boolean isScreenReceiverRegistered = false;
    private boolean isMyBroadcastRegistered = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        int statusBarHeight = 0;
        @SuppressLint("InternalInsetResource") int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        if (XPrefs.Xprefs.getBoolean("enableLauncherVibration", false)) {
            Class<?> AllAppsRecyclerView = findClassIfExists("com.android.launcher3.allapps.AllAppsRecyclerView", lpparam.classLoader);
            if (AllAppsRecyclerView != null) {
                tryHookAllMethods(AllAppsRecyclerView, "onUpdateScrollbar", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!((View) (param.thisObject)).canScrollVertically(1) || !((View) (param.thisObject)).canScrollVertically(-1)) {
                            if (!hasVibrated[0]) {
                                SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
                                hasVibrated[0] = true;
                            }
                        } else {
                            hasVibrated[0] = false;
                        }
                    }
                });
            }
            Class<?> QuickstepLauncher = findClassIfExists("com.android.launcher3.uioverrides.QuickstepLauncher", lpparam.classLoader);
            if (QuickstepLauncher != null) {
                tryHookAllMethods(QuickstepLauncher, "onStateSetEnd", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        hasVibrated[0] = true;
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("enableLauncherStatusVibration", false)) {
            final boolean[] canVibrate = {true};
            Class<?> StatusBarTouchController = findClassIfExists("com.android.launcher3.uioverrides.touchcontrollers.StatusBarTouchController", lpparam.classLoader);
            if (StatusBarTouchController != null) {
                tryHookAllMethods(StatusBarTouchController, "onControllerTouchEvent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (((MotionEvent) (param.args[0])).getActionMasked() == MotionEvent.ACTION_MOVE && canVibrate[0]) {
                            canVibrate[0] = false;
                            SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            canVibrate[0] = true;
                        }, 100);
                    }
                });
            }
            Class<?> HomeView = findClassIfExists("com.honeyspace.ui.honeypots.homescreen.presentation.HomeView", lpparam.classLoader);
            if (HomeView != null) {
                tryHookAllMethods(HomeView, "onTouchEvent", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (((MotionEvent) (param.args[0])).getActionMasked() == MotionEvent.ACTION_MOVE && canVibrate[0]) {
                            canVibrate[0] = false;
                            SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            canVibrate[0] = true;
                        }, 100);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("oneUiLauncherIconsPadding", false)) {
            Class<?> IconViewImpl = findClassIfExists("com.honeyspace.ui.common.iconview.IconViewImpl", lpparam.classLoader);
            if (IconViewImpl != null) {
                int textPadding = 24;
                tryHookAllMethods(IconViewImpl, "onLayout", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.setPadding(textPadding, view.getPaddingTop(), textPadding, view.getPaddingBottom());
                    }
                });
            }
        }
        float oneUiLauncherBgOpacity = Float.parseFloat(Xprefs.getString("oneUiLauncherBgOpacity", "0"));
        if (oneUiLauncherBgOpacity > 0) {
            XC_MethodHook oneUiLauncherBgOpacityHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    Drawable drawable = ResourcesCompat.getDrawable(XPrefs.modRes, R.drawable.app_drawer_bg, XPrefs.modRes.newTheme());
                    drawable.setAlpha((int) (255f * (oneUiLauncherBgOpacity / 100f)));
                    view.setBackground(drawable);
                }
            };
            Class<?> CustomApplistContainer = findClassIfExists("com.honeyspace.ui.honeypots.customapplist.presentation.CustomApplistContainer", lpparam.classLoader);
            if (CustomApplistContainer != null) {
                tryHookAllConstructors(CustomApplistContainer, oneUiLauncherBgOpacityHook);
            }
            Class<?> VerticalApplistContainer = findClassIfExists("com.honeyspace.ui.honeypots.verticalapplist.presentation.VerticalApplistContainer", lpparam.classLoader);
            if (VerticalApplistContainer != null) {
                tryHookAllConstructors(VerticalApplistContainer, oneUiLauncherBgOpacityHook);
            }
        }
        if (XPrefs.Xprefs.getBoolean("hideOneUiLauncherFastScroller", false)) {
            XC_MethodHook hideOneUiLauncherFastScroller = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    view.setTranslationX(500);
                }
            };
            Class<?> CustomApplistFastScroller = findClassIfExists("com.honeyspace.ui.honeypots.customapplist.presentation.CustomApplistFastScroller", lpparam.classLoader);
            if (CustomApplistFastScroller != null) {
                tryHookAllConstructors(CustomApplistFastScroller, hideOneUiLauncherFastScroller);
            }
            Class<?> VerticalApplistFastScroller = findClassIfExists("com.honeyspace.ui.honeypots.verticalapplist.presentation.VerticalApplistFastScroller", lpparam.classLoader);
            if (VerticalApplistFastScroller != null) {
                tryHookAllConstructors(VerticalApplistFastScroller, hideOneUiLauncherFastScroller);
            }
        }
        if (XPrefs.Xprefs.getBoolean("enableST2SLock", false)) {
            Class<?> WorkspaceTouchListener = findClassIfExists("com.android.launcher3.touch.WorkspaceTouchListener", lpparam.classLoader);
            if (WorkspaceTouchListener != null) {
                tryHookAllMethods(WorkspaceTouchListener, "onTouch", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent event = (MotionEvent) param.args[1];
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            Object mLauncher = getObjectField(param.thisObject, "mLauncher");
                            Object mStateManager = getObjectField(mLauncher, "mStateManager");
                            if (Objects.equals(getObjectField(mStateManager, "mCurrentStableState").toString(), "Normal")) {
                                canLock = true;
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    canLock = false;
                                }, 200);
                            }
                        }
                        if ((event.getAction() == MotionEvent.ACTION_UP) && canLock && ((boolean) (param.getResult()))) {
                            canLock = false;
                            try {
                                Runtime.getRuntime().exec("su -c input keyevent 223");
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                });
            }
            // One UI
            Class<?> Launcher = findClassIfExists("com.sec.android.app.launcher.Launcher", lpparam.classLoader);
            if (Launcher != null) {
                tryHookAllConstructors(Launcher, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLauncherOneUi = param.thisObject;
                    }
                });
            }
            Class<?> HomeView = findClassIfExists("com.honeyspace.ui.honeypots.homescreen.presentation.HomeView", lpparam.classLoader);
            if (HomeView != null) {
                final long[] initialTime = {-1};
                tryHookAllMethods(HomeView, "i", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.OnGestureListener() {
                            @Override
                            public boolean onDown(@NonNull MotionEvent motionEvent) {
                                initialTime[0] = System.currentTimeMillis();
                                return false;
                            }

                            @Override
                            public void onShowPress(@NonNull MotionEvent motionEvent) {
                            }

                            @Override
                            public boolean onScroll(@Nullable MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                                return false;
                            }

                            @Override
                            public void onLongPress(@NonNull MotionEvent motionEvent) {
                            }

                            @Override
                            public boolean onFling(@Nullable MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                                return false;
                            }

                            @Override
                            public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
                                if (System.currentTimeMillis() - initialTime[0] < 100) {
                                    Window window = (Window) callMethod(mLauncherOneUi, "getWindow");
                                    WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
                                    windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH);
                                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                                    cmd("input keyevent 223").submit();
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
                                    }, 400);
                                }
                                return false;
                            }
                        });
                        try {
                            setObjectField(param.thisObject, "g", gestureDetector);
                        } catch (Exception e) {
                            setObjectField(param.thisObject, "e", gestureDetector);
                        }
                    }
                });
            }
            Class<?> PageIndicatorView = findClassIfExists("com.honeyspace.ui.common.pageindicator.PageIndicatorView", lpparam.classLoader);
            if (PageIndicatorView != null) {
                tryHookAllConstructors(PageIndicatorView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup viewGroup = (ViewGroup) param.thisObject;
                        View touchBlock = new View(viewGroup.getContext());
                        touchBlock.setOnTouchListener(new View.OnTouchListener() {
                            @SuppressLint("ClickableViewAccessibility")
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return true;
                            }
                        });
                        viewGroup.addView(touchBlock);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("enableST2SMyAod", false)) {
            Class<?> WorkspaceTouchListener = findClassIfExists("com.android.launcher3.touch.WorkspaceTouchListener", lpparam.classLoader);
            if (WorkspaceTouchListener != null) {
                int finalStatusBarHeight = statusBarHeight;
                tryHookAllMethods(WorkspaceTouchListener, "onTouch", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        MotionEvent event = (MotionEvent) param.args[1];
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            canLock = true;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                canLock = false;
                            }, 200);
                        }
                        if ((event.getAction() == MotionEvent.ACTION_UP) && canLock && ((boolean) (param.getResult()))) {
                            canLock = false;
                            PackageInfo packageInfo = null;
                            try {
                                packageInfo = mContext.getPackageManager().getPackageInfo("uvnesh.myaod", GET_ACTIVITIES);
                            } catch (Exception ignored) {
                            }
                            Object mLauncher = getObjectField(param.thisObject, "mLauncher");
                            Helper.INSTANCE.playSound(mContext);
                            FrameLayout rootView = (FrameLayout) callMethod(mLauncher, "getRootView");
                            Window window = (Window) callMethod(mLauncher, "getWindow");
                            WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
                            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                            FrameLayout innerFrame = (FrameLayout) Helper.INSTANCE.addView(rootView, finalStatusBarHeight);
                            View blackView = new View(innerFrame.getContext());
                            blackView.setBackgroundColor(ContextCompat.getColor(innerFrame.getContext(), android.R.color.black));
                            View backgroundView = new View(innerFrame.getContext());
                            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x00000000, 0xFF000000});
                            backgroundView.setBackground(gradientDrawable);
                            innerFrame.addView(blackView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                            FrameLayout.LayoutParams bgLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                            innerFrame.addView(backgroundView, bgLayoutParams);
                            int frameHeight = rootView.getMeasuredHeight();
                            backgroundView.setY(-frameHeight);
                            int pixelsToAdjust = 1; // Additional Pixels to avoid slight gap
                            blackView.setY((-(frameHeight * 2)) + pixelsToAdjust);
                            long animDuration = 400;
                            ObjectAnimator bgFirst = ObjectAnimator.ofFloat(backgroundView, "y", backgroundView.getY(), frameHeight - pixelsToAdjust);
                            bgFirst.setDuration(animDuration);
                            ObjectAnimator blackFirst = ObjectAnimator.ofFloat(blackView, "y", blackView.getY(), 0);
                            blackFirst.setDuration(animDuration);
                            PackageInfo finalPackageInfo = packageInfo;
                            blackFirst.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    if (finalPackageInfo == null) {
                                        try {
                                            Runtime.getRuntime().exec("su -c input keyevent 223");
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        rootView.removeView(innerFrame);
                                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
                                    }, 400);
                                }
                            });
                            blackFirst.start();
                            bgFirst.start();
                            // Only Good for 0.5x Speed
                            if (packageInfo != null) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        Runtime.getRuntime().exec("su -c am start -n uvnesh.myaod/.MainActivity");
                                    } catch (Throwable ignored) {
                                    }
                                }, (long) (animDuration - (animDuration / 1.75)));
                            }
                        }
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("enableLauncherPage1", false)) {
            int toPage = 1;
            Class<?> Workspace = findClassIfExists("com.android.launcher3.Workspace", lpparam.classLoader);
            if (Workspace != null) {
                tryHookAllMethods(Workspace, "moveToDefaultScreen", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        moveToDefaultScreen(toPage);
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(Workspace, "onWindowFocusChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ((boolean) param.args[0]) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    callMethod(param.thisObject, "moveToDefaultScreen");
                                } catch (Throwable ignored) {
                                    moveToDefaultScreen(toPage);
                                }
                            }, 300);
                        }
                    }
                });
                tryHookAllMethods(Workspace, "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                callMethod(param.thisObject, "moveToDefaultScreen");
                            } catch (Throwable ignored) {
                                moveToDefaultScreen(toPage);
                            }
                        }, 300);
                    }
                });
                tryHookAllConstructors(Workspace, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        launcherWorkspaceObject = param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                callMethod(param.thisObject, "moveToDefaultScreen");
                            } catch (Throwable ignored) {
                                moveToDefaultScreen(toPage);
                            }
                        }, 300);
                    }
                });
//				onPageBeginTransition, onPageEndTransition, getDestinationPage
                tryHookAllMethods(Workspace, "onScrollChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isHomeTriggered) {
                            try {
                                callMethod(param.thisObject, "moveToDefaultScreen");
                            } catch (Throwable ignored) {
//								new Handler(Looper.getMainLooper()).postDelayed(() -> {
//									callMethod(param.thisObject, "scrollRight");
//									callMethod(param.thisObject, "snapToPageImmediately", 1);
//									callMethod(param.thisObject, "snapToPage", 1);
//								}, 80);
                                moveToDefaultScreen(toPage);
                            }
                        }
                    }
                });
            }
            Class<?> QuickstepLauncher = findClassIfExists("com.android.launcher3.uioverrides.QuickstepLauncher", lpparam.classLoader);
            if (QuickstepLauncher != null) {
                tryHookAllMethods(QuickstepLauncher, "onStateSetEnd", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (callMethod(param.args[0], "toString").equals("Hint")) {
                            isHomeTriggered = true;
                        } else if (isHomeTriggered && callMethod(param.args[0], "toString").equals("Normal")) {
                            try {
                                callMethod(launcherWorkspaceObject, "moveToDefaultScreen");
                            } catch (Throwable ignored) {
                                moveToDefaultScreen(toPage);
                            }
                            isHomeTriggered = false;
                        }
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("enableLauncherExtraPadding", false)) {
            Class<?> DeviceProfile = findClassIfExists("com.android.launcher3.DeviceProfile", lpparam.classLoader);
            if (DeviceProfile != null) {
                tryHookAllMethods(DeviceProfile, "getHorizontalMarginPx", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(((int) param.getResult()) * 3);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("launcherIncrease_numRows", false)) {
            Class<?> InvariantDeviceProfile = findClassIfExists("com.android.launcher3.InvariantDeviceProfile", lpparam.classLoader);
            if (InvariantDeviceProfile != null) {
                tryHookAllMethods(InvariantDeviceProfile, "initGrid", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "numRows", 6);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("launcherHideOverviewActions", false)) {
            Class<?> OverviewActionsView = findClassIfExists("com.android.quickstep.views.OverviewActionsView", lpparam.classLoader);
            if (OverviewActionsView != null) {
                tryHookAllMethods(OverviewActionsView, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.setVisibility(View.GONE);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("launcherOverviewActionsHeight0", false)) {
            Class<?> DeviceProfile = findClassIfExists("com.android.launcher3.DeviceProfile", lpparam.classLoader);
            if (DeviceProfile != null) {
                tryHookAllConstructors(DeviceProfile, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "overviewActionsHeight", 0);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("launcherScrimColorFix", false)) {
            Class<?> ActivityAllAppsContainerView = findClassIfExists("com.android.launcher3.allapps.ActivityAllAppsContainerView", lpparam.classLoader);
            if (ActivityAllAppsContainerView != null) {
                tryHookAllMethods(ActivityAllAppsContainerView, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean isLightMode = (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                        int bgColor = isLightMode ? 0xFFDADADA : 0xFF131313;
                        int searchColor = isLightMode ? 0xFFFFFFFF : 0xFF363636;
                        setObjectField(param.thisObject, "mBottomSheetBackgroundColorBlurFallback", bgColor);
//						setObjectField(param.thisObject, "mBottomSheetBackgroundColorLegacy", 0xFFFFFFFF);
//						setObjectField(param.thisObject, "mBottomSheetBackgroundColorOverBlur", 0xFFFFFFFF);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            View mSearchContainer = (View) getObjectField(param.thisObject, "mSearchContainer");
                            mSearchContainer.setBackgroundTintList(ColorStateList.valueOf(searchColor));
                        }, 1000);
                    }
                });
            }
        }
        boolean enable_taskbar_on_phones = XPrefs.Xprefs.getBoolean("enable_taskbar_on_phones", false);
        boolean isBatterySaverOnScreenOff = XPrefs.Xprefs.getBoolean("isBatterySaverOnScreenOff", false);
        if (enable_taskbar_on_phones || isBatterySaverOnScreenOff) {
            Class<?> StashedHandleView = findClassIfExists("com.android.launcher3.taskbar.StashedHandleView", lpparam.classLoader);
            if (StashedHandleView != null) {
                tryHookAllConstructors(StashedHandleView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isScreenReceiverRegistered) {
                            ScreenReceiver screenReceiver = new ScreenReceiver((View) param.thisObject, enable_taskbar_on_phones, isBatterySaverOnScreenOff);
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(Intent.ACTION_SCREEN_ON);
                            filter.addAction(Intent.ACTION_SCREEN_OFF);
                            mContext.getApplicationContext().registerReceiver(screenReceiver, filter);
                            isScreenReceiverRegistered = true;
                        } else {
                            if (enable_taskbar_on_phones) {
                                ScreenReceiver screenReceiver = new ScreenReceiver((View) param.thisObject, true, false);
                                IntentFilter filter = new IntentFilter();
                                filter.addAction(Intent.ACTION_SCREEN_ON);
                                filter.addAction(Intent.ACTION_SCREEN_OFF);
                                mContext.getApplicationContext().registerReceiver(screenReceiver, filter);
                            }
                        }
                    }
                });
            }
        }
        Class<?> StashedHandleView = findClassIfExists("com.android.launcher3.taskbar.StashedHandleView", lpparam.classLoader);
        if (StashedHandleView != null) {
            tryHookAllConstructors(StashedHandleView, registerMyReceiver());
        }
        Class<?> HomeView = findClassIfExists("com.honeyspace.ui.honeypots.homescreen.presentation.HomeView", lpparam.classLoader);
        if (HomeView != null) {
            tryHookAllConstructors(HomeView, registerMyReceiver());
        }
        if (XPrefs.Xprefs.getBoolean("enableLauncherQQS", false)) {
            XC_MethodHook onStatusBarTouchEvent = getOnStatusBarTouchEventHook();
            Class<?> SystemUiProxy = findClassIfExists("com.android.quickstep.SystemUiProxy", lpparam.classLoader);
            if (SystemUiProxy != null) {
                tryHookAllMethods(SystemUiProxy, "onStatusBarTouchEvent", onStatusBarTouchEvent);
            }
            Class<?> SystemUiProxyOneUI = findClassIfExists("L1.t", lpparam.classLoader);
            if (SystemUiProxyOneUI != null) {
                tryHookAllMethods(SystemUiProxyOneUI, "onStatusBarTouchEvent", onStatusBarTouchEvent);
            }
            Class<?> SystemUiProxyOneUI8 = findClassIfExists("X1.t", lpparam.classLoader);
            if (SystemUiProxyOneUI8 != null) {
                tryHookAllMethods(SystemUiProxyOneUI8, "onStatusBarTouchEvent", onStatusBarTouchEvent);
            }
        }
        if (XPrefs.Xprefs.getBoolean("launcherSearchUIFix", false)) {
            Class<?> AppsSearchContainerLayout = findClassIfExists("com.android.launcher3.allapps.search.AppsSearchContainerLayout", lpparam.classLoader);
            if (AppsSearchContainerLayout != null) {
                tryHookAllConstructors(AppsSearchContainerLayout, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        EditText editText = (EditText) param.thisObject;
                        editText.setHint("Search apps");
                        SpannableStringBuilder mSearchQueryBuilder = (SpannableStringBuilder) getObjectField(param.thisObject, "mSearchQueryBuilder");
                        Selection.removeSelection(mSearchQueryBuilder);
                        editText.setIncludeFontPadding(false);
                    }
                });
            }
        }
        if (XPrefs.Xprefs.getBoolean("oxygenOsLauncherFixes", false)) {
            Class<?> OplusFastScrollLayout = findClassIfExists("com.android.launcher3.allapps.OplusFastScrollLayout", lpparam.classLoader);
            if (OplusFastScrollLayout != null) {
                tryHookAllConstructors(OplusFastScrollLayout, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.setVisibility(View.GONE);
                    }
                });
            }
            Class<?> OplusBubbleTextView = findClassIfExists("com.android.launcher3.OplusBubbleTextView", lpparam.classLoader);
            if (OplusBubbleTextView != null) {
                int textPadding = 24;
                tryHookAllConstructors(OplusBubbleTextView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.addOnLayoutChangeListener((view1, i, i1, i2, i3, i4, i5, i6, i7) -> {
                            view.setPadding(textPadding, view.getPaddingTop(), textPadding, view.getPaddingBottom());
                        });
                    }
                });
            }
            Class<?> OplusWorkspace = findClassIfExists("com.android.launcher3.OplusWorkspace", lpparam.classLoader);
            if (OplusWorkspace != null) {
                int padding = 74;
                tryHookAllConstructors(OplusWorkspace, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        view.addOnLayoutChangeListener((view1, i, i1, i2, i3, i4, i5, i6, i7) -> {
                            view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
                        });
                    }
                });
            }
            Class<?> OplusHotseat = findClassIfExists("com.android.launcher3.OplusHotseat", lpparam.classLoader);
            if (OplusHotseat != null) {
                tryHookAllMethods(OplusHotseat, "calculateHotseatCellWidth", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(298);
                    }
                });
                tryHookAllMethods(OplusHotseat, "getScreenWidth", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(1586);
                    }
                });
            }
            Class<?> LauncherAppsSearchContainerLayout = findClassIfExists("com.android.launcher3.allapps.search.LauncherAppsSearchContainerLayout", lpparam.classLoader);
            if (LauncherAppsSearchContainerLayout != null) {
                tryHookAllConstructors(LauncherAppsSearchContainerLayout, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                                marginLayoutParams.setMarginStart(100);
                                marginLayoutParams.setMarginEnd(100);
                            } catch (Exception ignored) {
                            }
                        }, 1000);
                    }
                });
            }
            Class<?> OplusAllAppsRecyclerView = findClassIfExists("com.android.launcher3.allapps.OplusAllAppsRecyclerView", lpparam.classLoader);
            if (OplusAllAppsRecyclerView != null) {
                int horizontalPadding = 128;
                tryHookAllConstructors(OplusAllAppsRecyclerView, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                                    marginLayoutParams.topMargin = 230;
                                } catch (Exception ignored) {
                                }
                            }
                        }, 1000);
                        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                        viewTreeObserver.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                            @Override
                            public void onDraw() {
                                view.setPadding(horizontalPadding, view.getPaddingTop(), horizontalPadding, view.getPaddingBottom());
                            }
                        });
                        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                view.setPadding(horizontalPadding, view.getPaddingTop(), horizontalPadding, view.getPaddingBottom());
                                return true;
                            }
                        });
                        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                            @Override
                            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                                view.setPadding(horizontalPadding, view.getPaddingTop(), horizontalPadding, view.getPaddingBottom());
                            }
                        });
                    }
                });
            }
        }
//		if (XPrefs.Xprefs.getBoolean("launcherClearAllFix", false)) {
//			Class<?> RecentsView = findClassIfExists("com.android.quickstep.views.RecentsView", lpparam.classLoader);
//			if (RecentsView != null) {
//				tryHookAllConstructors(RecentsView, new XC_MethodHook() {
//					@Override
//					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//						Button mClearAllButton = (Button) getObjectField(param.thisObject, "mClearAllButton");
//						mClearAllButton.setAllCaps(false);
//					}
//				});
//			}
//		}
    }

    private XC_MethodHook registerMyReceiver() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (isMyBroadcastRegistered) {
                    return;
                }
                MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(MyBroadcastReceiver.SCREENSHOT);
                mContext.getApplicationContext().registerReceiver(myBroadcastReceiver, filter, Context.RECEIVER_EXPORTED);
                isMyBroadcastRegistered = true;
            }
        };
    }

    private XC_MethodHook getOnStatusBarTouchEventHook() {
        final boolean[] shouldExpandQQS = {false};
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MotionEvent event = (MotionEvent) param.args[0];
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int w = mContext.getResources().getDisplayMetrics().widthPixels;
                    shouldExpandQQS[0] = event.getX() >= w * 0.65;
                    if (shouldExpandQQS[0]) {
                        param.setResult(null);
                        try {
                            Runtime.getRuntime().exec("su -c cmd statusbar expand-settings");
                        } catch (Throwable ignored) {
                        }
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (shouldExpandQQS[0]) {
                        param.setResult(null);
                    }
                }
            }
        };
    }

    private void moveToDefaultScreen(int toPage) {
        if (launcherWorkspaceObject != null) {
            if (!(boolean) callMethod(launcherWorkspaceObject, "workspaceInModalState")) {
                callMethod(launcherWorkspaceObject, "snapToPage", toPage);
            }
            View view = (View) callMethod(launcherWorkspaceObject, "getChildAt", toPage);
            if (view != null) {
                view.requestFocus();
            }
        }
    }
}