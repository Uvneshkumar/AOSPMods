package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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

	private Boolean canLock = false;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		int statusBarHeight = 0;
		@SuppressLint("InternalInsetResource") int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
		}
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
		if (XPrefs.Xprefs.getBoolean("enableLauncherStatusVibration", false)) {
			Class<?> StatusBarTouchController = findClassIfExists("com.android.launcher3.uioverrides.touchcontrollers.StatusBarTouchController", lpparam.classLoader);
			if (StatusBarTouchController != null) {
				tryHookAllMethods(StatusBarTouchController, "dispatchTouchEvent", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (((MotionEvent) (param.args[0])).getActionMasked() == MotionEvent.ACTION_DOWN) {
							SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
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
							Intent intent = mContext.getPackageManager().getLaunchIntentForPackage("uvnesh.myaod");
							Object mLauncher = getObjectField(param.thisObject, "mLauncher");
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
							long animDuration = 500;
							ObjectAnimator bgFirst = ObjectAnimator.ofFloat(backgroundView, "y", backgroundView.getY(), frameHeight - pixelsToAdjust);
							bgFirst.setDuration(animDuration);
							ObjectAnimator blackFirst = ObjectAnimator.ofFloat(blackView, "y", blackView.getY(), 0);
							blackFirst.setDuration(animDuration);
							blackFirst.addListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									super.onAnimationEnd(animation);
									if (intent == null) {
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
							if (intent != null) {
								new Handler(Looper.getMainLooper()).postDelayed(() -> {
									mContext.startActivity(intent);
								}, (long) (animDuration - (animDuration / 1.25)));
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
				tryHookAllConstructors(Workspace, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setIntField(param.thisObject, "mCurrentPage", toPage);
					}
				});
				tryHookAllMethods(Workspace, "moveToDefaultScreen", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View view = (View) callMethod(param.thisObject, "getChildAt", toPage);
						if (view != null) {
							view.requestFocus();
						}
					}
				});
			}
			Class<?> PagedView = findClassIfExists("com.android.launcher3.PagedView", lpparam.classLoader);
			if (PagedView != null) {
				tryHookAllMethods(PagedView, "snapToPage", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (((int) param.args[0]) == 0) {
							param.args[0] = toPage;
						}
					}
				});
			}
		}
	}
}