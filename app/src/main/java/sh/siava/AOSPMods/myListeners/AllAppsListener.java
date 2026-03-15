package sh.siava.AOSPMods.myListeners;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.myLog;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.utils.Helpers.tryHookAllMethods;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.myListeners.helper.Helper;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class AllAppsListener extends XposedModPack {

    int STRONG_AUTH_NOT_REQUIRED = 0x0;

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
        // Disable (Pixel) Launcher Top Shadow
        Class<?> SysUiScrim = findClassIfExists("com.android.launcher3.graphics.SysUiScrim", lpparam.classLoader);
        if (SysUiScrim != null) {
            tryHookAllMethods(SysUiScrim, "draw", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
        }
        // https://github.com/AidanWarner97/frameworks_base/commit/cee0f07dad38c2dc93717548da8924b0c0989e64
        if (Xprefs.getBoolean("whiteBatteryIcon", false)) {
            Class<?> ThemedBatteryDrawable = findClassIfExists("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader);
            if (ThemedBatteryDrawable != null) {
                tryHookAllConstructors(ThemedBatteryDrawable, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setIntField(param.thisObject, "fillColor", Color.WHITE);
                        setIntField(param.thisObject, "backgroundColor", Color.WHITE);
                        setIntField(param.thisObject, "levelColor", Color.WHITE);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("requireStrongAuth", false)) {
            Class<?> LockPatternUtils = findClassIfExists("com.android.internal.widget.LockPatternUtils", lpparam.classLoader);
            if (LockPatternUtils != null) {
                tryHookAllMethods(LockPatternUtils, "requireStrongAuth", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
                            param.setResult(null);
                        }
                    }
                });
                tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(STRONG_AUTH_NOT_REQUIRED);
                    }
                });
                tryHookAllMethods(LockPatternUtils, "isTrustAllowedForUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
            Class<?> StrongAuthTracker = findClassIfExists("com.android.server.trust.TrustManagerService.StrongAuthTracker", lpparam.classLoader);
            if (StrongAuthTracker != null) {
                tryHookAllMethods(StrongAuthTracker, "isTrustAllowedForUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
            Class<?> LockSettingsService = findClassIfExists("com.android.server.locksettings.LockSettingsService", lpparam.classLoader);
            if (LockSettingsService != null) {
                tryHookAllMethods(LockSettingsService, "requireStrongAuth", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
                            param.setResult(null);
                        }
                    }
                });
                tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(STRONG_AUTH_NOT_REQUIRED);
                    }
                });
            }
            Class<?> LockSettingsStrongAuth = findClassIfExists("com.android.server.locksettings.LockSettingsStrongAuth", lpparam.classLoader);
            if (LockSettingsStrongAuth != null) {
                tryHookAllMethods(LockSettingsStrongAuth, "handleScheduleNonStrongBiometricIdleTimeout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(LockSettingsStrongAuth, "requireStrongAuth", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if ((int) param.args[0] != STRONG_AUTH_NOT_REQUIRED) {
                            param.setResult(null);
                        }
                    }
                });
                tryHookAllMethods(LockPatternUtils, "getStrongAuthForUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(STRONG_AUTH_NOT_REQUIRED);
                    }
                });
            }
            Class<?> TrustAgentService = findClassIfExists("android.service.trust.TrustAgentService", lpparam.classLoader);
            if (TrustAgentService != null) {
                tryHookAllMethods(TrustAgentService, "onTrustTimeout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
                        } catch (Exception ignored) {
                        }
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(TrustAgentService, "grantTrust", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    }
                });
                tryHookAllMethods(TrustAgentService, "revokeTrust", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(TrustAgentService, "lockUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
            Class<?> TrustAgentWrapper = findClassIfExists("com.android.server.trust.TrustAgentWrapper", lpparam.classLoader);
            if (TrustAgentWrapper != null) {
                tryHookAllMethods(TrustAgentWrapper, "onTrustTimeout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
                        } catch (Exception ignored) {
                        }
                        param.setResult(null);
                    }
                });
            }
            Class<?> GoogleTrustAgentChimeraService = findClassIfExists("com.google.android.gms.trustagent.GoogleTrustAgentChimeraService", lpparam.classLoader);
            if (GoogleTrustAgentChimeraService != null) {
                tryHookAllMethods(GoogleTrustAgentChimeraService, "onTrustTimeout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
                        } catch (Exception ignored) {
                        }
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(GoogleTrustAgentChimeraService, "grantTrust", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    }
                });
                tryHookAllMethods(GoogleTrustAgentChimeraService, "revokeTrust", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(GoogleTrustAgentChimeraService, "lockUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
            Class<?> GoogleChimeraTrustAgentService = findClassIfExists("com.google.android.chimera.TrustAgentService", lpparam.classLoader);
            if (GoogleChimeraTrustAgentService != null) {
                tryHookAllMethods(GoogleChimeraTrustAgentService, "onTrustTimeout", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            callMethod(param.thisObject, "grantTrust", "Kept unlocked by Extend Unlock", 0, 8);
                        } catch (Exception ignored) {
                        }
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(GoogleChimeraTrustAgentService, "grantTrust", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    }
                });
                tryHookAllMethods(GoogleChimeraTrustAgentService, "revokeTrust", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
                tryHookAllMethods(GoogleChimeraTrustAgentService, "lockUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("hideNavIcons", false)) {
            Class<?> NavigationBarView = findClassIfExists("android.inputmethodservice.navigationbar.NavigationBarView", lpparam.classLoader);
            if (NavigationBarView != null) {
                tryHookAllMethods(NavigationBarView, "updateNavButtonIcons", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object backButton = callMethod(param.thisObject, "getBackButton");
                        callMethod(backButton, "setVisibility", View.INVISIBLE);
                    }
                });
            }
        }
        Class<?> RecentTasks = findClassIfExists("com.android.server.wm.RecentTasks", lpparam.classLoader);
        if (RecentTasks != null) {
            tryHookAllMethods(RecentTasks, "add", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0].toString().contains("uvnesh.myaod") || param.args[0].toString().contains("com.iprototypes.volume") || param.args[0].toString().contains("uvnesh.lockwidget") || param.args[0].toString().contains("uvnesh.power")) {
                        param.setResult(null);
                    }
                }
            });
        }
        if (Xprefs.getBoolean("oneHandedCornerRadiusReduce", false)) {
            Class<?> Transaction = findClassIfExists("android.view.SurfaceControl.Transaction", lpparam.classLoader);
            if (Transaction != null) {
                tryHookAllMethods(Transaction, "setCornerRadius", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // rounded_corner_radius - extra
                        // Komodo
                        if (((float) param.args[1]) == 166.0f) {
                            param.args[1] = ((float) param.args[1]) - 40.0f;
                        }
                        // S20 FHD
                        else if (((float) param.args[1]) == 130.0f) {
                            param.args[1] = ((float) param.args[1]) - 30.0f;
                        }
                    }
                });
            }
        }
        if (Xprefs.getBoolean("disableBatteryTime", false)) {
            Class<?> BatteryControllerImpl = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);
            if (BatteryControllerImpl != null) {
                tryHookAllMethods(BatteryControllerImpl, "updatePercentText", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int mShowPercentMode = getIntField(param.thisObject, "mShowPercentMode");
                        if (mShowPercentMode == 3) {
                            setIntField(param.thisObject, "mShowPercentMode", 1);
                            callMethod(param.thisObject, "updateShowPercent");
                            callMethod(param.thisObject, "updatePercentText");
                            param.setResult(null);
                        }
                    }
                });
            }
        }
        Class<?> AppCompatAspectRatioPolicy = findClassIfExists("com.android.server.wm.AppCompatAspectRatioPolicy", lpparam.classLoader);
        if (AppCompatAspectRatioPolicy != null) {
            tryHookAllMethods(AppCompatAspectRatioPolicy, "applyAspectRatio", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String appPackageName = (String) getObjectField(getObjectField(param.thisObject, "mActivityRecord"), "packageName");
                    if (appPackageName.equals("com.crater.bbtan")) {
                        param.setResult(false);
                    }
                }
            });
        }
        if (Xprefs.getBoolean("oneUiPermCrashFix", false)) {
            Class<?> AppOpsManagerCompat = findClassIfExists("com.android.permissioncontroller.permission.compat.AppOpsManagerCompat", lpparam.classLoader);
            if (AppOpsManagerCompat != null) {
                tryHookAllMethods(AppOpsManagerCompat, "checkOpRawNoThrow", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(callMethod(param.args[0], "unsafeCheckOpRawNoThrow", param.args[1], param.args[2], param.args[3]));
                    }
                });
            }
            Class<?> PermissionMapping = findClassIfExists("com.android.permissioncontroller.permission.utils.PermissionMapping", lpparam.classLoader);
            if (PermissionMapping != null) {
                tryHookAllMethods(PermissionMapping, "getPlatformPermissionsOfGroup", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(Helper.INSTANCE.getPlatformPermissionsOfGroup(getStaticObjectField(PermissionMapping, "PLATFORM_PERMISSION_GROUPS"), (PackageManager) param.args[0], (String) param.args[1]));
                    }
                });
            }
        }
        boolean enableHapticTextHandle2 = Xprefs.getBoolean("enableHapticTextHandle2", false);
        boolean chirpScreenUnlockVibration = Xprefs.getBoolean("chirpScreenUnlockVibration", false);
        if (enableHapticTextHandle2 || chirpScreenUnlockVibration) {
            Class<?> SystemVibratorManager = findClassIfExists("android.os.SystemVibratorManager", lpparam.classLoader);
            tryHookAllMethods(SystemVibratorManager, "vibrate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (enableHapticTextHandle2 && param.args[2].toString().contains("effect=TEXTURE_TICK")) {
                        param.args[2] = CombinedVibration.createParallel(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                    }
                    if (chirpScreenUnlockVibration && param.args[1].toString().contains("com.android.systemui") && param.args[2].toString().contains("Composed{segments=[Step{amplitude=0.39215687, frequencyHz=0.0, duration=5}, Step{amplitude=0.0, frequencyHz=0.0, duration=52}, Step{amplitude=0.039215688, frequencyHz=0.0, duration=10}, Step{amplitude=1.0, frequencyHz=0.0, duration=10}, Step{amplitude=0.078431375, frequencyHz=0.0, duration=10}], repeat=-1}")) {
//                        param.args[2] = CombinedVibration.createParallel(VibrationEffect.createWaveform(new long[]{0, 5, 52, 30}, -1));
                        param.setResult(null);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (chirpScreenUnlockVibration && param.args[1].toString().contains("com.android.systemui") && param.args[2].toString().contains("Composed{segments=[Step{amplitude=0.39215687, frequencyHz=0.0, duration=5}, Step{amplitude=0.0, frequencyHz=0.0, duration=52}, Step{amplitude=0.039215688, frequencyHz=0.0, duration=10}, Step{amplitude=1.0, frequencyHz=0.0, duration=10}, Step{amplitude=0.078431375, frequencyHz=0.0, duration=10}], repeat=-1}")) {
                        new Handler(Looper.getMainLooper()).post(() -> SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH));
                        new Handler(Looper.getMainLooper()).postDelayed(() -> SystemUtils.vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, VibrationAttributes.USAGE_TOUCH), 52);
                    }
                }
            });
        }
        String[] fpIconPosition_array = Xprefs.getString("fpIconPosition", "").split(",");
        if (fpIconPosition_array.length == 3) {
            int sensorLocationX = Integer.parseInt(fpIconPosition_array[0].trim());
            int sensorLocationY = Integer.parseInt(fpIconPosition_array[1].trim());
            int sensorRadius = Integer.parseInt(fpIconPosition_array[2].trim());
            Class<?> SensorLocationInternal = findClassIfExists("android.hardware.biometrics.SensorLocationInternal", lpparam.classLoader);
            if (SensorLocationInternal != null) {
                tryHookAllConstructors(SensorLocationInternal, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "sensorLocationX", sensorLocationX);
                        setObjectField(param.thisObject, "sensorLocationY", sensorLocationY);
                        setObjectField(param.thisObject, "sensorRadius", sensorRadius);
                    }
                });
            }
        }
        if (Xprefs.getBoolean("experimentalDisableThermalThrottle", false)) {
            Class<?> ThermalManagerService = findClassIfExists("com.android.server.power.thermal.ThermalManagerService", lpparam.classLoader);
            if (ThermalManagerService != null) {
                tryHookAllMethods(ThermalManagerService, "onTemperatureChanged", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object temperature = param.args[0];
//                        if (temperature.toString().contains("mType=3, mName=SKIN")) {
                        myLog("Before: " + temperature);
                        setObjectField(temperature, "mValue", 35f);
                        setObjectField(temperature, "mStatus", 0);
                        myLog("After: " + temperature);
                        param.args[0] = temperature;
//                        }
                    }
                });
            }
        }

//        Class<?> PackageImpl = findClassIfExists("com.android.internal.pm.parsing.pkg.PackageImpl", lpparam.classLoader);
//        if (PackageImpl != null) {
//            tryHookAllMethods(PackageImpl, "setOnBackInvokedCallbackEnabled", new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    String packageName = (String) getObjectField(param.thisObject, "packageName");
//                    if (Objects.equals(packageName, "com.whatsapp")) {
//                        param.args[0] = true;
//                    }
//                }
//            });
//        }

//        Display Colour CF Lumen
//        Class<?> DisplayTransfosrmManager = findClassIfExists("com.android.server.display.color.DisplayTransformManager", lpparam.classLoader);
//        if (DisplayTransformManager != null) {
//            tryHookAllMethods(DisplayTransformManager, "setColorMatrix", new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    if (((int) param.args[0]) == 300) {
//                        DisplayEngineController displayEngineController = new DisplayEngineController();
//                        displayEngineController.updateBalance();
//                        callMethod(param.thisObject, "setColorMatrix", displayEngineController.getLevel(), displayEngineController.getMatrix());
//                    }
//                }
//            });
//        }

//		Vibration
//		Class<?> SystemVibrator = findClassIfExists("android.os.SystemVibrator", lpparam.classLoader);
//		hookEverything(SystemVibrator);
//		Class<?> VibrationAttributes = findClassIfExists("android.os.VibrationAttributes", lpparam.classLoader);
//		hookEverything(VibrationAttributes);
//
//		Class<?> VibrationEffectClass = findClassIfExists("android.os.VibrationEffect", lpparam.classLoader);
//		hookEverything(VibrationEffectClass);
//		tryHookAllMethods(VibrationEffectClass, "createPredefined", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				myLog("createPredefined");
//				if (((int) param.args[0]) == VibrationEffect.EFFECT_CLICK) {
//					param.args[0] = VibrationEffect.EFFECT_TICK;
//				}
//			}
//		});
//		tryHookAllMethods(VibrationEffectClass, "createWaveform", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				myLog("createWaveform");
//			}
//		});
//		tryHookAllMethods(VibrationEffectClass, "createOneShot", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				myLog("createOneShot");
//			}
//		});
//		tryHookAllMethods(VibrationEffectClass, "createRepeatingEffect", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				myLog("createRepeatingEffect");
//			}
//		});
//
//		Class<?> Vibrator = findClassIfExists("android.os.Vibrator", lpparam.classLoader);
//		hookEverything(Vibrator);
//		tryHookAllMethods(Vibrator, "arePrimitivesSupported", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				before_arePrimitivesSupported(param);
//			}
//		});
//		tryHookAllMethods(Vibrator, "areAllPrimitivesSupported", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				param.setResult(true);
//			}
//		});
//		tryHookAllMethods(Vibrator, "getPrimitiveDurations", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				before_getPrimitiveDurations(param);
//			}
//		});
//
//		Class<?> VibratorManager = findClassIfExists("android.os.VibratorManager", lpparam.classLoader);
//		hookEverything(VibratorManager);
//		Class<?> SystemVibratorManager = findClassIfExists("android.os.SystemVibratorManager", lpparam.classLoader);
//		hookEverything(SystemVibratorManager);
//
//		Class<?> VibrationEffect$Composition = findClassIfExists("android.os.VibrationEffect$Composition", lpparam.classLoader);
//		hookEverything(VibrationEffect$Composition);
//		tryHookAllMethods(VibrationEffect$Composition, "compose", new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//				// noinspection unchecked
//				ArrayList<Object> mSegments = (ArrayList<Object>) getObjectField(param.thisObject, "mSegments");
//				myLog(mSegments.size());
//				double CLICK_AMPLITUDE = 1.0;
//				double TICK_AMPLITUDE = 1.0;
//				double LOW_TICK_AMPLITUDE = 1.0;
//				Pattern pattern = Pattern.compile("Primitive\\{primitive=([A-Z_]+), scale=([\\d.]+), delay=(\\d+), delayType=([A-Z_]+)\\}");
//				long[] timings = new long[mSegments.size() * 2];
//				int[] amplitudes = new int[mSegments.size() * 2];
//				int currentIndex = 0;
//				for (int i=0; i< mSegments.size(); i++) {
//					Object mSegment = mSegments.get(i);
//					myLog(mSegment);
//					Matcher matcher = pattern.matcher(mSegment.toString().trim());
//					if (matcher.matches()) {
//						String primitive = matcher.group(1);
//						float scale = Float.parseFloat(matcher.group(2));
//						int delay = Integer.parseInt(matcher.group(3));
//						timings[currentIndex] = delay;
//						amplitudes[currentIndex] = 0;
//						currentIndex++;
//						if (Objects.equals(primitive, "CLICK")) {
//							timings[currentIndex] = 20;
//							amplitudes[currentIndex] = (int) (255 * CLICK_AMPLITUDE * scale);
//						}
//						else if (Objects.equals(primitive, "TICK")) {
//							timings[currentIndex] = 30;
//							amplitudes[currentIndex] = (int) (255 * TICK_AMPLITUDE * scale);
//						}
//						else if (Objects.equals(primitive, "LOW_TICK")) {
//							timings[currentIndex] = 10;
//							amplitudes[currentIndex] = (int) (255 * LOW_TICK_AMPLITUDE * scale);
//						}
//						currentIndex++;
//					}
//				}
//				if (currentIndex != 0) {
//					VibrationEffect vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, -1);
//					param.setResult(vibrationEffect);
//				}
//			}
//		});
//
//		Class<?> VibratorInfo = findClassIfExists("android.os.VibratorInfo", lpparam.classLoader);
//		hookEverything(VibratorInfo);

//		Recents Vibration
//		Class<?> VibratorWrapper = findClassIfExists("com.android.launcher3.util.VibratorWrapper", lpparam.classLoader);
//		if (VibratorWrapper != null) {
//			tryHookAllMethods(VibratorWrapper, "vibrate", new XC_MethodHook() {
//				@Override
//				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//					if (param.args[0].toString().equals("Composed{segments=[Prebaked{effect=CLICK, strength=MEDIUM, fallback=true}], repeat=-1, mMagnitudeType=TYPE_EXTRA}")) {
//						VibrationEffect vibrationEffect = VibrationEffect.createOneShot(10, 255);
//						SystemUtils.vibrate(vibrationEffect, VibrationAttributes.USAGE_TOUCH);
//						param.setResult(null);
//					}
//				}
//			});
//		}
    }

//	public void before_arePrimitivesSupported(XC_MethodHook.MethodHookParam param) {
//		int[] primitiveIds = (int[]) param.args[0];
//		boolean[] supported = new boolean[primitiveIds.length];
//		for (int i = 0; i < primitiveIds.length; i++) {
//			supported[i] = true;
//		}
//		param.setResult(supported);
//	}
//
//	public void before_getPrimitiveDurations(XC_MethodHook.MethodHookParam param) {
//		int[] primitiveIds = (int[]) param.args[0];
//		int[] durations = new int[primitiveIds.length];
//		for (int i = 0; i < primitiveIds.length; i++) {
//			durations[i] = 1000;
//		}
//		param.setResult(durations);
//	}
}
