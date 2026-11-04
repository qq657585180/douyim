package com.jingfei.douyin.hook;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解除弹幕屏蔽词 30 条限制（抖X danmaku.a1ce67ce92）。
 *
 * 抖音 40.8 的弹幕屏蔽词面板（DanmakuBlockingWordsListPortraitPanel /
 * DanmakuBlockingWordsListLandscapePanel）在刷新时会把"添加屏蔽词（N/30）"
 * 文本写回，并在 N>=30 时把按钮置灰（alpha=0.35f）。
 * 本类 Hook 这两个面板的刷新方法（40.8 混淆名 LM/zM），在宿主刷新后：
 *  1. 把文本改为"添加屏蔽词（N）"，隐藏 "/30" 上限显示；
 *  2. 达到 30 条后强制恢复按钮可用（enabled/clickable/alpha=1.0），
 *     使面板不再因达到上限而禁用添加入口。
 */
final class DanmakuBlockWordPanelEnhancer {

    /** 抖音 40.8 弹幕屏蔽词面板类名（竖屏/横屏，未混淆）。 */
    private static final String[] PANEL_CLASSES = {
            "com.ss.android.ugc.aweme.feed.danmaku.ui.DanmakuBlockingWordsListPortraitPanel",
            "com.ss.android.ugc.aweme.feed.danmaku.ui.DanmakuBlockingWordsListLandscapePanel"
    };

    /** 面板刷新方法名（40.8 反编译得到，均为 public final 无参 void）。 */
    private static final String[] REFRESH_METHODS = {"LM", "zM"};

    /** 匹配 "添加屏蔽词（N/30）" 或 "添加屏蔽词（N）"。 */
    private static final Pattern BLOCK_WORD_PATTERN =
            Pattern.compile("添加屏蔽词（(\\d+)(?:/30)?）");

    /** 已处理 TextView 的当前数量缓存，避免同一数量反复 setText。 */
    private static final Map<TextView, Integer> HANDLED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean enabled;

    private DanmakuBlockWordPanelEnhancer() {
    }

    /** 由 PlaybackStrategy 注入开关（danmaku.a1ce67ce92）。 */
    static void applyPrefs(boolean removeLimitEnabled) {
        enabled = removeLimitEnabled;
        if (!enabled) {
            HANDLED.clear();
        }
    }

    static void install(DouyinModule module, ClassLoader loader) {
        int hooked = 0;
        for (String className : PANEL_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className, false, loader);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (isRefreshMethod(method)) {
                        hookRefresh(module, method);
                        hooked++;
                    }
                }
            } catch (Throwable error) {
                module.log(Log.WARN, DouyinModule.TAG,
                        "DanmakuBlockWordPanel: " + className + " -> " + error);
            }
        }
        module.log(Log.INFO, DouyinModule.TAG,
                "DanmakuBlockWordPanel hooked " + hooked + " refresh methods");
    }

    private static boolean isRefreshMethod(Method method) {
        if (method.getParameterCount() != 0
                || method.getReturnType() != Void.TYPE) {
            return false;
        }
        String name = method.getName();
        for (String candidate : REFRESH_METHODS) {
            if (candidate.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void hookRefresh(DouyinModule module, Method method) {
        module.hook(method)
                .setId("douyin-danmaku-blockword-" + method.toGenericString())
                .setExceptionMode(DouyinModule.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (enabled) {
                        refreshPanel(chain.getThisObject());
                    }
                    return result;
                });
    }

    /** 面板刷新后修改"添加屏蔽词"按钮。 */
    private static void refreshPanel(Object fragment) {
        if (fragment == null) {
            return;
        }
        try {
            Method getView = fragment.getClass().getMethod("getView");
            Object view = getView.invoke(fragment);
            if (view instanceof View) {
                updateTextView((View) view);
            }
        } catch (Throwable ignored) {
            // 面板视图未创建或方法签名变化时跳过
        }
    }

    private static void updateTextView(View root) {
        if (root == null) {
            return;
        }
        if (root instanceof TextView) {
            apply((TextView) root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateTextView(group.getChildAt(i));
            }
        }
    }

    private static void apply(TextView tv) {
        CharSequence cs = tv.getText();
        if (cs == null) {
            return;
        }
        Matcher matcher = BLOCK_WORD_PATTERN.matcher(cs.toString());
        if (!matcher.find()) {
            return;
        }
        int count;
        try {
            count = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return;
        }
        Integer previous = HANDLED.get(tv);
        if (previous == null || previous != count) {
            // 隐藏 "/30" 上限，只显示真实数量
            tv.setText("添加屏蔽词（" + count + "）");
            HANDLED.put(tv, count);
        }
        // 达到 30 条后宿主会把按钮置灰（alpha 0.35），强制恢复可用
        if (count >= 30) {
            tv.setEnabled(true);
            tv.setClickable(true);
            tv.setAlpha(1.0f);
        }
    }
}
