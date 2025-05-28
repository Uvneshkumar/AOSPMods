package sh.siava.AOSPMods.myListeners;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.Helper;
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
	private View stashedHandleView = null;

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
								SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_TOUCH);
								hasVibrated[0] = true;
							}
						} else {
							hasVibrated[0] = false;
						}
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
							SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_TOUCH);
						}
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
							canVibrate[0] = true;
						}, 100);
					}
				});
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
							canLock = true;
							new Handler(Looper.getMainLooper()).postDelayed(() -> {
								canLock = false;
							}, 200);
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
						if (!(boolean) callMethod(param.thisObject, "workspaceInModalState")) {
							callMethod(param.thisObject, "snapToPage", toPage);
						}
						View view = (View) callMethod(param.thisObject, "getChildAt", toPage);
						if (view != null) {
							view.requestFocus();
						}
						param.setResult(null);
					}
				});
				tryHookAllMethods(Workspace, "onWindowFocusChanged", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if ((boolean) param.args[0]) {
							new Handler(Looper.getMainLooper()).postDelayed(() -> {
								callMethod(param.thisObject, "moveToDefaultScreen");
							}, 300);
						}
					}
				});
				tryHookAllMethods(Workspace, "onAttachedToWindow", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
							callMethod(param.thisObject, "moveToDefaultScreen");
						}, 300);
					}
				});
				tryHookAllConstructors(Workspace, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
							callMethod(param.thisObject, "moveToDefaultScreen");
						}, 300);
					}
				});
//				onPageBeginTransition, onPageEndTransition, getDestinationPage
				tryHookAllMethods(Workspace, "onScrollChanged", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (isHomeTriggered) {
							callMethod(param.thisObject, "moveToDefaultScreen");
							isHomeTriggered = false;
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
		if (XPrefs.Xprefs.getBoolean("enable_taskbar_on_phones", false)) {
			Class<?> StashedHandleView = findClassIfExists("com.android.launcher3.taskbar.StashedHandleView", lpparam.classLoader);
			if (StashedHandleView != null) {
				tryHookAllConstructors(StashedHandleView, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						stashedHandleView = (View) param.thisObject;
					}
				});
			}
			Class<?> TaskbarKeyguardController = findClassIfExists("com.android.launcher3.taskbar.TaskbarKeyguardController", lpparam.classLoader);
			if (TaskbarKeyguardController != null) {
				ViewTreeObserver.OnPreDrawListener stashedHandleViewPreDrawListener = () -> {
					if (stashedHandleView.getAlpha() == 1f) {
						stashedHandleView.setAlpha(0f);
					}
					return true;
				};
				tryHookAllMethods(TaskbarKeyguardController, "updateStateForSysuiFlags", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if ((Long) param.args[0] == 537001984 && stashedHandleView != null) {
							ViewTreeObserver viewTreeObserver = stashedHandleView.getViewTreeObserver();
							viewTreeObserver.addOnPreDrawListener(stashedHandleViewPreDrawListener);
							Helper.INSTANCE.animateAlphaReverse(stashedHandleView);
							new Handler(Looper.getMainLooper()).postDelayed(() -> {
								viewTreeObserver.removeOnPreDrawListener(stashedHandleViewPreDrawListener);
							}, 400);
						}
					}
				});
			}
		}
	}
}