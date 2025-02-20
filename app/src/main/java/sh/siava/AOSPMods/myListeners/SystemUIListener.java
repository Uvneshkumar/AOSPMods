package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.service.notification.StatusBarNotification;
import android.view.GestureDetector;
import android.view.Gravity;
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

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.Helper;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class SystemUIListener extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	long pulseDelay = 1000;
	final Handler handler = new Handler(Looper.myLooper());
	Runnable runnable = null;

	final Handler handler2 = new Handler(Looper.myLooper());
	Runnable runnable2 = null;

	boolean isAodIconVisible = true;

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

	private final StringFormatter stringFormatter = new StringFormatter();
	private Object QSFV;
	private final StringFormatter.formattedStringCallback refreshCallback = this::setQSFooterText;

	public final String CLIPBOARD_OVERLAY_SHOW_ACTIONS = "clipboard_overlay_show_actions";
	public final String NAMESPACE_SYSTEMUI = "systemui";

	private static final int SHADE = 0; // frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarState.java - screen unlocked - pulsing means screen is locked - shade locked means (Q)QS is open on lockscreen
	GestureDetector mLockscreenDoubleTapToSleep; // event callback for double tap to sleep detection of statusbar only
	private Object NotificationPanelViewController;

	private boolean doubleTap;

	private void adjustClockMargin(XC_MethodHook.MethodHookParam param) {
		TextView textView = (TextView) param.thisObject;
		if (!textView.isSingleLine()) {
			textView.setPadding(0, 0, 0, Helper.INSTANCE.getPx(80));
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
									Helper.INSTANCE.setNotificationIcon((ImageView) param.thisObject, (StatusBarNotification) getObjectField(param.thisObject, "mNotification"), true);
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
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						FrameLayout rootView = (FrameLayout) param.thisObject;
						ImageView myIcon = new ImageView(mContext);
						myIcon.setImageDrawable(Helper.INSTANCE.createOvalDrawable());
						int iconPadding = 5;
						myIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
						rootView.addView(myIcon);
						FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) myIcon.getLayoutParams();
						lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
						lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
						lp.gravity = Gravity.CENTER;
						myIcon.setLayoutParams(lp);
						rootView.setScaleX(1.3f);
						rootView.setScaleY(1.3f);
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
						int iconPadding = 10;
						myIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
							int whiteColor = Color.parseColor("#FFFFFF");
							myIcon.setImageTintList(ColorStateList.valueOf(whiteColor));
						}, 1000);
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
									Helper.INSTANCE.setNotificationIcon((ImageView) param.args[0], (StatusBarNotification) getObjectField(param.args[0], "mNotification"), true);
								}, 1000);
							}
						}
					}
				});
			}
		}
		if (Xprefs.getBoolean("disallowDeepAODBetter", false)) {
			XC_MethodHook noDozeHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((int) param.args[0] == 3) {
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
							for (int i = 0; i < childCount; i++) {
								if (view.getChildAt(i).toString().contains("app:id/burn_in_layer")) {
									burn_in_layer = view.getChildAt(i);
								}
								if (view.getChildAt(i).toString().contains("app:id/keyguard_slice_view")) {
									keyguard_slice_view = view.getChildAt(i);
								}
							}
							if (burn_in_layer != null && keyguard_slice_view != null) {
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
