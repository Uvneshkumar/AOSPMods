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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.service.notification.StatusBarNotification;
import android.view.Display;
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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
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
									Helper.INSTANCE.setNotificationIcon((ImageView) param.args[0], (StatusBarNotification) getObjectField(param.args[0], "mNotification"), true);
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
		if (Xprefs.getBoolean("whiteLockClockAOD", false)) {
			Class<?> SimpleDigitalClockTextView = findClassIfExists("com.android.systemui.shared.clocks.view.SimpleDigitalClockTextView", lpparam.classLoader);
			if (SimpleDigitalClockTextView != null) {
				tryHookAllConstructors(SimpleDigitalClockTextView, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View view = (View) param.thisObject;
						setObjectField(view, "aodColor", 0xFFFFFFFF);
					}
				});
				tryHookAllMethods(SimpleDigitalClockTextView, "updateColor", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View view = (View) param.thisObject;
						setObjectField(view, "aodColor", 0xFFFFFFFF);
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
		if (largeClockTopMarginA16 || largeClockDateSmartSpaceTopMarginA16 || hideClockDateSmartSpaceA16) {
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
					@SuppressLint("ClickableViewAccessibility")
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
						float nothingLockIconScale = Float.parseFloat(Xprefs.getString("nothingLockIconScale", "1"));
						rootView.setScaleX(nothingLockIconScale);
						rootView.setScaleY(nothingLockIconScale);
						if (Xprefs.getBoolean("nothingLockIconLongPress", false)) {
							new Handler(Looper.getMainLooper()).postDelayed(() -> {
								String viewName;
								if (Xprefs.getBoolean("nothingLockIconLongPressCrashFix", false)) {
									viewName = "longPressHandlingView";
								} else {
									viewName = "touchHandlingView";
								}
								View touchHandlingView = (View) getObjectField(param.thisObject, viewName);
								Object listener = getObjectField(touchHandlingView, "listener");
								tryHookAllMethods(listener.getClass(), "onLongPressDetected", new XC_MethodHook() {
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
									callMethod(listener, "onLongPressDetected", touchHandlingView, false);
									return false;
								});
							}, 1000);
						}
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
						if (!(boolean) getObjectField(NotificationPanelViewController, "mPulsing")
								&& !(boolean) getObjectField(NotificationPanelViewController, "mDozing")
								&& (int) getObjectField(NotificationPanelViewController, "mBarState") == SHADE
								&& (boolean) callMethod(NotificationPanelViewController, "isFullyCollapsed")) {
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
		if (Xprefs.getBoolean("pixelAODBrightness", false)) {
			float pixelAODBrightness = Float.parseFloat(Xprefs.getString("pixelAODBrightnessFloat", "0.03"));
			Class<?> DozeService = findClassIfExists("com.android.systemui.doze.DozeService", lpparam.classLoader);
			if (DozeService != null) {
				tryHookAllMethods(DozeService, "setDozeScreenBrightnessFloat", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.args[0] = pixelAODBrightness;
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
//		if (Xprefs.getBoolean("noNotificationsAllCapsFix", false)) {
//			Class<?> EmptyShadeView = findClassIfExists("com.android.systemui.statusbar.notification.emptyshade.ui.view.EmptyShadeView", lpparam.classLoader);
//			if (EmptyShadeView != null) {
//				tryHookAllMethods(EmptyShadeView, "onFinishInflate", new XC_MethodHook() {
//					@Override
//					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//						TextView mEmptyText = (TextView) getObjectField(param.thisObject, "mEmptyText");
//						if (mEmptyText != null) {
//							mEmptyText.setAllCaps(false);
//						}
//					}
//				});
//			}
//		}
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
				tryHookAllConstructors(KeyguardSliceView, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						ViewGroup viewGroup = (ViewGroup) param.thisObject;
						viewGroup.addView(new CustomDateAlarmLayout(mContext));
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

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
	}
}
