package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class SystemUIListener extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	final Handler handler = new Handler(Looper.myLooper());
	Runnable runnable = null;

	private void initializeRunnable(Object thisObject) {
		handler.removeCallbacks(runnable);
		runnable = new Runnable() {
			public void run() {
				// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/packages/SystemUI/src/com/android/systemui/doze/DozeLog.java;l=557;drc=70468495b83418eb4a406b91daed502c74709745#:~:text=556-,557,-558
				callMethod(thisObject, "requestPulse", 9, true, null);
				handler.postDelayed(this, 1000);
			}
		};
	}

	public SystemUIListener(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	private final StringFormatter stringFormatter = new StringFormatter();
	private Object QSFV;
	private final StringFormatter.formattedStringCallback refreshCallback = this::setQSFooterText;

	// Seems like an executor, but doesn't act! perfect thing
	ExecutorService notExecutor = new ExecutorService() {
		@Override
		public void shutdown() {
		}

		@Override
		public List<Runnable> shutdownNow() {
			return null;
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return null;
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return null;
		}

		@Override
		public Future<?> submit(Runnable task) {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
			return null;
		}

		@Override
		public void execute(Runnable command) {
		}
	};

	public final String CLIPBOARD_OVERLAY_SHOW_ACTIONS = "clipboard_overlay_show_actions";
	public final String NAMESPACE_SYSTEMUI = "systemui";

	private static final int SHADE = 0; // frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarState.java - screen unlocked - pulsing means screen is locked - shade locked means (Q)QS is open on lockscreen
	GestureDetector mLockscreenDoubleTapToSleep; // event callback for double tap to sleep detection of statusbar only
	private Object NotificationPanelViewController;

	private boolean doubleTap;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;
		if (Xprefs.getBoolean("disableLocationPrivacyIndicator", false)) {
			Class<?> PrivacyConfig = findClassIfExists("com.android.systemui.privacy.PrivacyConfig", lpparam.classLoader);
			if (PrivacyConfig != null) {
				tryHookAllMethods(PrivacyConfig, "isLocationEnabled", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(false);
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
			Class<?> NotificationIconContainer = findClassIfExists("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader);
			if (NotificationIconContainer != null) {
				tryHookAllMethods(NotificationIconContainer, "onViewAdded", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						aodNotificationIcons(param);
					}
				});
				tryHookAllMethods(NotificationIconContainer, "onViewRemoved", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						aodNotificationIcons(param);
					}
				});
			}
		}
		if (Xprefs.getBoolean("disableBatteryTime", false)) {
			Class<?> BatteryControllerImpl = findClassIfExists("com.android.systemui.statusbar.policy.BatteryControllerImpl", lpparam.classLoader);
			if (BatteryControllerImpl != null) {
				tryHookAllMethods(BatteryControllerImpl, "generateTimeRemainingString", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(null);
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
		if (Xprefs.getBoolean("whiteLockClock", false)) {
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
		if (Xprefs.getBoolean("hideLockScreenStatusBar", false)) {
			Class<?> KeyguardStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);
			if (KeyguardStatusBarView != null) {
				tryHookAllMethods(KeyguardStatusBarView, "loadDimens", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View mSystemIconsContainer = (View) getObjectField(param.thisObject, "mSystemIconsContainer");
						callMethod(mSystemIconsContainer, "setVisibility", View.INVISIBLE);
					}
				});
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
				tryHookAllMethods(QSTileImplClass, "click", vibrateCallback);
				tryHookAllMethods(QSTileImplClass, "longClick", vibrateCallback);
			}
		}
		if (Xprefs.getBoolean("hideBuildNumber", false)) {
			stringFormatter.registerCallback(refreshCallback);
			Class<?> QSFooterViewClass = findClassIfExists("com.android.systemui.qs.QSFooterView", lpparam.classLoader);
			if (QSFooterViewClass != null) {
				tryHookAllConstructors(QSFooterViewClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						QSFV = param.thisObject;
					}
				});
				tryHookAllMethods(QSFooterViewClass, "setBuildText", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setQSFooterText();
					}
				});
			}
		}
		if (Xprefs.getBoolean("disableScreenshotSound", false)) {
			Class<?> ScreenshotControllerClass = findClassIfExists("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader);
			if (ScreenshotControllerClass != null) {
				tryHookAllConstructors(ScreenshotControllerClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setObjectField(param.thisObject, "mBgExecutor", notExecutor);
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
			mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onDoubleTap(MotionEvent e) {
					SystemUtils.Sleep();
					return true;
				}
			});
			Class<?> PhoneStatusBarViewControllerClass = findClassIfExists("com.android.systemui.statusbar.phone.PhoneStatusBarViewController", lpparam.classLoader);
			if (PhoneStatusBarViewControllerClass != null) {
				hookTouchHandler(PhoneStatusBarViewControllerClass); // 13 QPR3
			}
			Class<?> NotificationPanelViewControllerClass = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
			if (NotificationPanelViewControllerClass != null) {
				tryHookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						NotificationPanelViewController = param.thisObject;
						try {
							hookTouchHandler(getObjectField(param.thisObject, "mStatusBarViewTouchEventHandler").getClass());
						} catch (Throwable ignored) {
						}
					}
				});
				tryHookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						NotificationPanelViewController = param.thisObject;
						hookTouchHandler(param.getResult().getClass());
					}
				});
			}
		}
		if (Xprefs.getBoolean("dt2sLockScreen", false)) {
			Class<?> NotificationShadeWindowViewControllerClass = findClassIfExists("com.android.systemui.shade.NotificationShadeWindowViewController", lpparam.classLoader);
			if (NotificationShadeWindowViewControllerClass != null) {
				tryHookAllConstructors(NotificationShadeWindowViewControllerClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						new Thread(() -> {
							try {
								Thread.sleep(5000); // for some reason lsposed doesn't find methods in the class. so we'll hook to constructor and wait a bit!
							} catch (Exception ignored) {
							}
							setHooks(param);
						}).start();
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
						handler.postDelayed(runnable, 1000);
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
		}
	}

	private void setQSFooterText() {
		try {
			if (Xprefs.getBoolean("hideBuildNumber", false)) {
				TextView mBuildText = (TextView) getObjectField(QSFV, "mBuildText");
				setObjectField(QSFV, "mShouldShowBuildText", "".trim().length() > 0);
				mBuildText.setText(stringFormatter.formatString(""));
				mBuildText.setSelected(true);
			} else {
				callMethod(QSFV, "setBuildText");
			}
		} catch (Throwable ignored) {
		} //probably not initiated yet
	}

	private boolean hasDot(ViewGroup viewGroup) {
		boolean hasDot = false;
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View child = viewGroup.getChildAt(i);
			if (child.toString().contains("visibleState=DOT")) {
				hasDot = true;
				break;
			}
		}
		return hasDot;
	}

	float scaleFactor = 1.3f;

	private boolean hasDotAt(ViewGroup viewGroup, int position) {
		boolean isDotAtLast = false;
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View child = viewGroup.getChildAt(i);
			if (child.toString().contains("visibleState=DOT") && i == position) {
				isDotAtLast = true;
				break;
			}
		}
		return isDotAtLast;
	}

	private float getDP(float dip) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, Resources.getSystem().getDisplayMetrics());
	}

	private void increaseWidth(ViewGroup viewGroup, int currentWidth) {
		if (currentWidth >= (Resources.getSystem().getDisplayMetrics().widthPixels / scaleFactor) - 100) {
			increaseWidth(viewGroup, getInitialWidth(viewGroup));
			return;
		}
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (currentWidth + getDP(1f)), ViewGroup.LayoutParams.WRAP_CONTENT);
		viewGroup.setLayoutParams(layoutParams);
		viewGroup.post(() -> {
			if (hasDot(viewGroup)) {
				increaseWidth(viewGroup, (int) (currentWidth + getDP(1f)));
			}
		});
	}

	private void increaseWidthForLast(ViewGroup viewGroup, int currentWidth, int lastIndex) {
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (currentWidth + getDP(1f)), ViewGroup.LayoutParams.WRAP_CONTENT);
		viewGroup.setLayoutParams(layoutParams);
		viewGroup.post(() -> {
			if (!hasDotAt(viewGroup, lastIndex)) {
				increaseWidthForLast(viewGroup, (int) (currentWidth + getDP(1f)), lastIndex);
			}
		});
	}

	private int getInitialWidth(ViewGroup viewGroup) {
		return 0;
	}

	private void aodNotificationIcons(XC_MethodHook.MethodHookParam param) {
		ViewGroup viewGroup = ((ViewGroup) (param.thisObject));
		if (getBooleanField(param.thisObject, "mOnLockScreen")) {
			int mMaxIconsOnAod = getIntField(param.thisObject, "mMaxIconsOnAod");
			viewGroup.setPadding(0, 0, 0, 0);
			((LinearLayout) (viewGroup.getParent())).setGravity(Gravity.CENTER);
			int totalChildren = viewGroup.getChildCount();
			int initialWidth = getInitialWidth(viewGroup);
			viewGroup.setScaleX(scaleFactor);
			viewGroup.setScaleY(scaleFactor);
//			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(initialWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (totalChildren * getDP(26)), ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.setMargins(100, 0, 100, 0);
			viewGroup.setLayoutParams(layoutParams);
//			if (totalChildren <= mMaxIconsOnAod && totalChildren > 0) {
//				increaseWidth(viewGroup, initialWidth);
//			} else if (totalChildren > mMaxIconsOnAod) {
//				increaseWidthForLast(viewGroup, initialWidth, mMaxIconsOnAod);
//			}
		}
	}

	private void hookTouchHandler(Class<?> TouchHanlderClass) {
		XC_MethodHook touchHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!(Xprefs.getBoolean("dt2sStatusBar", false))) return;
				// double tap to sleep, statusbar only
				try {
					if (!(boolean) getObjectField(NotificationPanelViewController, "mPulsing") && !(boolean) getObjectField(NotificationPanelViewController, "mDozing") && (int) getObjectField(NotificationPanelViewController, "mBarState") == SHADE && (boolean) callMethod(NotificationPanelViewController, "isFullyCollapsed")) {
						mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[param.args.length - 1]);
					}
				} catch (Throwable ignored) {
				}
			}
		};
		tryHookAllMethods(TouchHanlderClass, "onTouch", touchHook); // 13 QPR2
		tryHookAllMethods(TouchHanlderClass, "handleTouchEvent", touchHook); // A13 R18
	}

	private void setHooks(XC_MethodHook.MethodHookParam param) {
		try {
			Object mPulsingWakeupGestureHandler = getObjectField(param.thisObject, "mPulsingWakeupGestureHandler"); // A13 R18
			Object mListener = getObjectField(mPulsingWakeupGestureHandler, "mListener");
			Object mStatusBarKeyguardViewManager = getObjectField(param.thisObject, "mStatusBarKeyguardViewManager");
			Object mStatusBarStateController = getObjectField(param.thisObject, "mStatusBarStateController");
			XC_MethodHook doubleTapHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
					boolean isQSExpanded;
					try { // 13 QPR3
						isQSExpanded = getBooleanField(getObjectField(NotificationPanelViewController, "mQsController"), "mExpanded");
					} catch (Throwable ignored) {
						isQSExpanded = getBooleanField(NotificationPanelViewController, "mQsExpanded"); // 13 QPR2, 1
					}
					if (isQSExpanded || getBooleanField(NotificationPanelViewController, "mBouncerShowing")) {
						return;
					}
					doubleTap = true;
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							doubleTap = false;
						}
					}, 500 * 2);
				}
			};
			tryHookAllMethods(mListener.getClass(), "onDoubleTapEvent", doubleTapHook); // A13 R18
			tryHookAllMethods(mListener.getClass(), "onDoubleTap", doubleTapHook); // older
			// detect DTS on lockscreen
			tryHookAllMethods(mPulsingWakeupGestureHandler.getClass(), "onTouchEvent", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
					try {
						if (keyguardNotShowing(mStatusBarKeyguardViewManager)) {
							return;
						}
						MotionEvent ev = (MotionEvent) param1.args[0];
						int action = ev.getActionMasked();
						if (doubleTap && action == MotionEvent.ACTION_UP) {
							if ((Xprefs.getBoolean("dt2sLockScreen", false)) && !((boolean) callMethod(mStatusBarStateController, "isDozing")))
								SystemUtils.Sleep();
						}
					} catch (Throwable ignored) {
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}

	private boolean keyguardNotShowing(Object mStatusBarKeyguardViewManager) {
		try {
			return !((boolean) callMethod(mStatusBarKeyguardViewManager, "isShowing"));
		} catch (Throwable ignored) {
			return !getBooleanField(mStatusBarKeyguardViewManager, "mLastShowing");
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
	}
}
