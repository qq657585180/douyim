package com.jingfei.douyin.hook;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class PlayerHooks {
    private static final String[] ENGINE_CLASSES = {
            "com.ss.ttvideoengine.TTVideoEngine",
            "com.ss.ttvideoengine.TTVideoEngineImpl",
            "com.ss.ttvideoengine.TTVideoEngineImplV2",
            "com.ss.android.ugc.aweme.player.sdk.impl.SimplifyPlayerImpl",
            "com.ss.android.ugc.playerkit.simapicommon.model.SimVideoUrlModel"
    };

    private static final Set<Class<?>> HOOKED_LISTENER_CLASSES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, WeakReference<Object>> LISTENER_OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile float playbackSpeed = 1f;
    private static volatile boolean tempSpeedEnabled;
    private static volatile float tempSpeedFactor = 1.5f;
    private static volatile boolean tempSpeedActive;
    private static volatile String playbackQuality;
    private static volatile String liveQuality;

    /** 抖X"倍速"设置注入（0.25x–3.0x，默认 1x）。 */
    static void setPlaybackSpeed(float speed) {
        if (speed < 0.25f) {
            speed = 0.25f;
        } else if (speed > 3f) {
            speed = 3f;
        }
        playbackSpeed = speed;
    }

    /** 抖X"默认播放画质"设置注入（page.quality）。值可能是数字或"高清/超清"等字符串。 */
    static void setPlaybackQuality(String quality) {
        playbackQuality = quality == null ? null : quality.trim();
    }

    /** 抖X"直播默认画质"设置注入（live.41fb1b555b）。 */
    static void setLivePlaybackQuality(String quality) {
        liveQuality = quality == null ? null : quality.trim();
    }

    /** 抖X"临时倍速"设置注入（gestures.be54e501cb）。 */
    static void setTempSpeed(float factor, boolean enabled) {
        if (factor < 0.25f) {
            factor = 0.25f;
        } else if (factor > 3f) {
            factor = 3f;
        }
        tempSpeedFactor = factor;
        tempSpeedEnabled = enabled;
        if (!enabled) {
            tempSpeedActive = false;
        }
    }

    /** 长按激活/取消临时倍速（由触摸 Hook 调用）。 */
    static void setTemporarySpeedActive(boolean active) {
        if (!tempSpeedEnabled || tempSpeedActive == active) {
            return;
        }
        tempSpeedActive = active;
        Object player = PlaybackState.engine();
        if (player != null) {
            applySpeedValue(player, active ? tempSpeedFactor : playbackSpeed);
        }
    }

    private PlayerHooks() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        int hooked = 0;
        for (String className : ENGINE_CLASSES) {
            try {
                Class<?> type = Class.forName(className, false, loader);
                hooked += hookEngineClass(module, type);
            } catch (ClassNotFoundException ignored) {
                module.log(Log.DEBUG, DouyinModule.TAG, "player class absent: " + className);
            } catch (Throwable error) {
                module.log(Log.WARN, DouyinModule.TAG,
                        "player hook failed for " + className, error);
            }
        }
        module.log(Log.INFO, DouyinModule.TAG, "installed " + hooked + " player hooks");
    }

    private static int hookEngineClass(DouyinModule module, Class<?> type) {
        int count = 0;
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            String lower = name.toLowerCase();

            if (isPlayMethod(lower, method)) {
                hookState(module, method, true);
                count++;
            } else if (isPauseMethod(lower, method)) {
                hookState(module, method, false);
                count++;
            } else if (lower.contains("completion") || lower.contains("completed")) {
                hookEngineCompletion(module, method);
                count++;
            } else if (lower.startsWith("set") && lower.contains("listener")) {
                hookListenerSetter(module, method);
                count++;
            }
        }
        return count;
    }

    private static boolean isPlayMethod(String name, Method method) {
        return method.getParameterCount() == 0
                && (name.equals("play") || name.equals("start") || name.equals("resume"));
    }

    private static boolean isPauseMethod(String name, Method method) {
        return method.getParameterCount() == 0
                && (name.startsWith("pause")
                || name.equals("stop")
                || name.equals("release")
                || name.equals("releaseasync"));
    }

    private static void hookState(DouyinModule module, Method method, boolean playing) {
        module.hook(method)
                .setId("douyin-player-state-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (playing) {
                        Object player = chain.getThisObject();
                        PlaybackState.playing(player);
                        applySpeed(player);
                        applyQuality(player);
                    } else {
                        PlaybackState.paused(chain.getThisObject());
                    }
                    return result;
                });
    }

    private static void hookEngineCompletion(DouyinModule module, Method method) {
        module.hook(method)
                .setId("douyin-player-complete-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    PlaybackState.completed(chain.getThisObject());
                    return result;
                });
    }

    private static void hookListenerSetter(DouyinModule module, Method method) {
        module.hook(method)
                .setId("douyin-player-listener-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Object owner = chain.getThisObject();
                    for (Object argument : chain.getArgs()) {
                        if (argument != null) {
                            LISTENER_OWNERS.put(argument, new WeakReference<>(owner));
                            hookListenerObject(module, argument);
                        }
                    }
                    return result;
                });
    }

    private static void hookListenerObject(DouyinModule module, Object listener) {
        Class<?> type = listener.getClass();
        synchronized (HOOKED_LISTENER_CLASSES) {
            if (!HOOKED_LISTENER_CLASSES.add(type)) {
                return;
            }
        }

        int count = 0;
        for (Method method : type.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                continue;
            }
            String lower = method.getName().toLowerCase();
            try {
                if (lower.equals("oncompletion") || lower.equals("oncompleted")) {
                    hookListenerCompletion(module, method);
                    count++;
                } else if (lower.equals("onplaybackstatechanged")) {
                    hookPlaybackStateCallback(module, method);
                    count++;
                }
            } catch (Throwable error) {
                module.log(Log.WARN, DouyinModule.TAG,
                        "listener method hook failed: " + method, error);
            }
        }
        module.log(Log.DEBUG, DouyinModule.TAG,
                "listener " + type.getName() + ": " + count + " hooks");
    }

    private static void hookListenerCompletion(DouyinModule module, Method method) {
        module.hook(method)
                .setId("douyin-player-listener-complete-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    WeakReference<Object> ownerReference =
                            LISTENER_OWNERS.get(chain.getThisObject());
                    Object owner = ownerReference == null ? null : ownerReference.get();
                    if (owner != null) {
                        PlaybackState.completed(owner);
                    }
                    return result;
                });
    }

    private static void hookPlaybackStateCallback(DouyinModule module, Method method) {
        module.hook(method)
                .setId("douyin-player-callback-state-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Integer state = lastInteger(chain.getArgs());
                    if (state != null && state >= 0 && state <= 4) {
                        WeakReference<Object> ownerReference =
                                LISTENER_OWNERS.get(chain.getThisObject());
                        Object owner = ownerReference == null
                                ? null
                                : ownerReference.get();
                        // TTVideoEngine 39.7.0: 0=stopped, 1=playing,
                        // 2=paused, 3=error, 4=initializing.
                        if (owner != null && state == 1) {
                            PlaybackState.playingFromCallback(owner);
                        } else if (owner != null && state == 2) {
                            PlaybackState.paused(owner);
                        } else if (owner != null && state == 3) {
                            PlaybackState.error(owner);
                        }
                    }
                    return result;
                });
    }

    private static Integer lastInteger(java.util.List<Object> values) {
        for (int index = values.size() - 1; index >= 0; index--) {
            Object value = values.get(index);
            if (value instanceof Integer integer) {
                return integer;
            }
        }
        return null;
    }
    /**
     * 播放开始时应用抖X倍速。反射常见速率方法：
     * setRate(float) / setPlaybackRate(float) / setSpeed(float)。
     * 失败静默（不同版本方法名不同，找不到不影响播放）。
     */
    private static void applySpeed(Object player) {
        if (player == null) {
            return;
        }
        float speed = tempSpeedActive ? tempSpeedFactor : playbackSpeed;
        if (speed <= 0f || Float.compare(speed, 1f) == 0) {
            return;
        }
        applySpeedValue(player, speed);
    }

    private static void applySpeedValue(Object player, float speed) {
        if (player == null || speed <= 0f || Float.compare(speed, 1f) == 0) {
            return;
        }
        Method method = findRateMethod(player.getClass());
        if (method == null) {
            return;
        }
        try {
            method.invoke(player, speed);
        } catch (Throwable ignored) {
            // 播放器可能不支持该速率，忽略
        }
    }

    /**
     * 播放开始时应用抖X默认画质。反射常见画质方法：
     * setVideoQuality(int|String) / setQuality(int) / setPlaybackQuality(int)
     * / setResolution(String)。失败静默（不同版本方法名不同，不影响播放）。
     */
    private static void applyQuality(Object player) {
        if (player == null) {
            return;
        }
        // 直播画质优先于普通播放画质（直播播放器若复用同一引擎则生效）
        String quality = (liveQuality != null && !liveQuality.isEmpty())
                ? liveQuality : playbackQuality;
        if (quality == null || quality.isEmpty()) {
            return;
        }
        Method method = findQualityMethod(player.getClass());
        if (method == null) {
            return;
        }
        try {
            if (method.getParameterTypes()[0] == int.class) {
                method.invoke(player, parseQualityInt(quality));
            } else {
                method.invoke(player, quality);
            }
        } catch (Throwable ignored) {
            // 播放器可能不支持该画质值，忽略
        }
    }

    private static int parseQualityInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            switch (value.toLowerCase()) {
                case "流畅":
                case "sd":
                case "low":
                case "smooth":
                    return 1;
                case "高清":
                case "hd":
                    return 2;
                case "超清":
                case "fullhd":
                case "1080p":
                    return 3;
                case "原画":
                case "original":
                case "4k":
                    return 4;
                default:
                    return -1;
            }
        }
    }

    private static Method QUALITY_METHOD;

    private static Method findQualityMethod(Class<?> type) {
        Method cached = QUALITY_METHOD;
        if (cached != null && cached.getDeclaringClass().isAssignableFrom(type)) {
            return cached;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 1) {
                    String lower = method.getName().toLowerCase();
                    Class<?> paramType = method.getParameterTypes()[0];
                    boolean numeric = paramType == int.class || paramType == Integer.class;
                    boolean textual = paramType == String.class;
                    if ((numeric || textual)
                            && (lower.equals("setvideoquality")
                            || lower.equals("setquality")
                            || lower.equals("setplaybackquality")
                            || lower.equals("setresolution"))) {
                        try {
                            method.setAccessible(true);
                        } catch (Throwable ignored) {
                            // 非公开方法不可访问时继续尝试公开方法
                        }
                        QUALITY_METHOD = method;
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private static Method RATE_METHOD;

    private static Method findRateMethod(Class<?> type) {
        Method cached = RATE_METHOD;
        if (cached != null && cached.getDeclaringClass().isAssignableFrom(type)) {
            return cached;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == float.class) {
                    String lower = method.getName().toLowerCase();
                    if (lower.equals("setrate")
                            || lower.equals("setplaybackrate")
                            || lower.equals("setspeed")) {
                        try {
                            method.setAccessible(true);
                        } catch (Throwable ignored) {
                            // 非公开方法不可访问时继续尝试公开方法
                        }
                        RATE_METHOD = method;
                        return method;
                    }
                }
            }
        }
        return null;
    }
}
