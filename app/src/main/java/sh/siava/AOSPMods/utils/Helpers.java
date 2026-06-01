package sh.siava.AOSPMods.utils;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.graphics.RectF;
import android.media.AudioManager;
import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import sh.siava.AOSPMods.XPrefs;

public class Helpers {

    public static void dumpClass(String className, ClassLoader classLoader) {
        Class<?> ourClass = findClassIfExists(className, classLoader);
        if (ourClass == null) {
            log("Class: " + className + " not found");
            return;
        }
        dumpClass(ourClass);
    }

    public static void dumpClass(Class<?> ourClass) {
        Method[] ms = ourClass.getDeclaredMethods();
        log("Class: " + ourClass.getName());
        log("extends: " + ourClass.getSuperclass().getName());
        log("Subclasses:");
        Class<?>[] scs = ourClass.getClasses();
        for (Class<?> c : scs) {
            log(c.getName());
        }
        log("Methods:");

        Constructor<?>[] cons = ourClass.getDeclaredConstructors();
        for (Constructor<?> m : cons) {
            log(m.getName() + " - " + " - " + m.getParameterCount());
            Class<?>[] cs = m.getParameterTypes();
            for (Class<?> c : cs) {
                log("\t\t" + c.getTypeName());
            }
        }


        for (Method m : ms) {
            log(m.getName() + " - " + m.getReturnType() + " - " + m.getParameterCount());
            Class<?>[] cs = m.getParameterTypes();
            for (Class<?> c : cs) {
                log("\t\t" + c.getTypeName());
            }
        }
        log("Fields:");

        Field[] fs = ourClass.getDeclaredFields();
        for (Field f : fs) {
            log("\t\t" + f.getName() + "-" + f.getType().getName());
        }
        log("End dump");
    }

    private static final Set<String> EXCLUDED_METHODS = Set.of(
            "dummyMethodToSkipHook1",
            "dummyMethodToSkipHook2"
    );

    public static void hookEverything(Class<?> ourClass) {
        Method[] ms = ourClass.getDeclaredMethods();
        tryHookAllConstructors(ourClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                StringBuilder sb = new StringBuilder();
                sb.append(" \n");
                sb.append("Class: ").append(ourClass.getName()).append("\n");
                sb.append("Constructor:\n");
                for (Object arg : param.args) {
                    sb.append("    arg: ").append(arg).append("\n");
                }
                sb.append("    result: ").append(param.getResult());
                myLog(sb.toString());
            }
        });
        for (Method m : ms) {
            final String methodName = m.getName();
            if (EXCLUDED_METHODS.contains(methodName)) {
                continue;
            }
            tryHookAllMethods(ourClass, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" \n");
                    sb.append("Class: ").append(ourClass.getName()).append("\n");
                    sb.append("Method: ").append(methodName).append("\n");
                    for (Object arg : param.args) {
                        sb.append("    arg: ").append(arg).append("\n");
                    }
                    sb.append("    result: ").append(param.getResult());
                    myLog(sb.toString());
                }
            });
        }
    }

    private static FileObserver fileObserver;
    private static final File tri_state = new File("/proc/tristatekey/tri_state");
    private static String currentState = null;

    private static void observeAlertSlider() {
        if (!XPrefs.Xprefs.getBoolean("hookAlertSlider", false)) {
            return;
        }
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        fileObserver = new FileObserver(tri_state) {
            @Override
            public void onEvent(int i, @Nullable String s) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(tri_state));
                    String state = reader.readLine().trim();
                    if (!state.equals(currentState)) {
                        currentState = state;
                        AudioManager audioManager = SystemUtils.AudioManager();
                        if (audioManager == null) return;
                        int mode;
                        switch (state) {
                            case "1":
                                mode = AudioManager.RINGER_MODE_SILENT;
                                break;
                            case "2":
                                mode = AudioManager.RINGER_MODE_VIBRATE;
                                break;
                            case "3":
                                mode = AudioManager.RINGER_MODE_NORMAL;
                                break;
                            default:
                                return;
                        }
                        audioManager.setRingerMode(mode);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        fileObserver.startWatching();
    }

    public static void tryHookAllMethods(Class<?> clazz, String method, XC_MethodHook hook) {
        try {
            hookAllMethods(clazz, method, hook);
        } catch (Throwable ignored) {
        }
        observeAlertSlider();
    }

    public static void tryHookAllConstructors(Class<?> clazz, XC_MethodHook hook) {
        try {
            hookAllConstructors(clazz, hook);
        } catch (Throwable ignored) {
        }
        observeAlertSlider();
    }

    public static void myLog(Object text) {
//		log("- " + text + " -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//		log("Uvnesh: " + text);
        log("" + text);
    }

    public static void myLogOther(Object text) {
        Log.i("LSPosed-Bridge", text.toString());
    }

    public static RectF getRectF(String fp) {
        String[] split = fp.split(",");
        int x = Integer.parseInt(split[0].trim());
        int y = Integer.parseInt(split[1].trim());
        int radius = Integer.parseInt(split[2].trim()) / 2;
        return new RectF(x - radius, y - radius, x + radius, y + radius);
    }

    public static String getFpLocation() {
        return XPrefs.Xprefs.getString("disableLockScreenBounceFPLocation", "540, 1762, 280");
    }

    public static RectF getFpRect() {
        return getRectF(getFpLocation());
    }

}
