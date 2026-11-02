package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 播放进度时间叠加显示（抖X playback.progress_time 及 time_* 样式组）。
 * 在播放页面左下/中/右下叠加显示当前播放位置 / 总时长，
 * 时间位置、字号、颜色、透明度、边距均可由设置控制。
 */
final class PlaybackTimeOverlay {
    private static volatile boolean enabled;
    private static volatile String position = "right";
    private static volatile int sizeSp = 14;
    private static volatile int color = Color.WHITE;
    private static volatile int opacityPercent = 80;
    private static volatile int marginDp = 12;
    private static volatile int bottomDp = 48;

    private static WeakReference<TextView> overlayRef;
    private static Context appContext;
    private static Method currentPositionMethod;
    private static Method durationMethod;

    private PlaybackTimeOverlay() {
    }

    /** 由 PlaybackStrategy 注入样式参数；enabled=false 时移除叠加层。 */
    static void applyPrefs(boolean en, String pos, int size, int col,
                           int opacity, int margin, int bottom) {
        enabled = en;
        if (pos != null && !pos.isEmpty()) {
            position = pos;
        }
        if (size > 0) {
            sizeSp = size;
        }
        if (col != 0) {
            color = col;
        }
        if (opacity >= 0 && opacity <= 100) {
            opacityPercent = opacity;
        }
        if (margin >= 0) {
            marginDp = margin;
        }
        if (bottom >= 0) {
            bottomDp = bottom;
        }
        if (!enabled) {
            remove();
        }
    }

    /** 由 ImmersiveUi 播放页扫描循环调用，刷新当前播放位置。 */
    static void update(Activity activity) {
        if (!enabled || activity == null) {
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) {
                return;
            }
            TextView tv = resolveOverlay((ViewGroup) decor);
            if (tv == null) {
                return;
            }
            Object player = PlaybackState.engine();
            long pos = invokeLong(player, "getCurrentPosition", currentPositionMethod);
            long dur = invokeLong(player, "getDuration", durationMethod);
            if (pos < 0 && dur <= 0) {
                return;
            }
            if (pos < 0) {
                pos = 0;
            }
            if (dur < 0) {
                dur = 0;
            }
            tv.setText(format(pos) + (dur > 0 ? " / " + format(dur) : ""));
        } catch (Throwable ignored) {
            // 视图树变化或播放器接口差异时跳过
        }
    }

    private static TextView resolveOverlay(ViewGroup decor) {
        if (overlayRef != null) {
            TextView cached = overlayRef.get();
            if (cached != null && cached.getParent() != null) {
                return cached;
            }
            overlayRef = null;
        }
        Context context = decor.getContext();
        appContext = context.getApplicationContext();
        TextView tv = new TextView(context);
        tv.setIncludeFontPadding(false);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(color);
        tv.setAlpha(opacityPercent / 100f);
        tv.setBackgroundColor(0x66000000);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        int gravity = "left".equals(position) ? Gravity.LEFT
                : "center".equals(position) ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                gravity | Gravity.BOTTOM);
        lp.setMargins(dp(marginDp), 0, dp(marginDp), dp(bottomDp));
        decor.addView(tv, lp);
        overlayRef = new WeakReference<>(tv);
        return tv;
    }

    static void remove() {
        if (overlayRef == null) {
            return;
        }
        TextView tv = overlayRef.get();
        overlayRef = null;
        if (tv != null && tv.getParent() instanceof ViewGroup) {
            ((ViewGroup) tv.getParent()).removeView(tv);
        }
    }

    private static long invokeLong(Object target, String name, Method cached) {
        if (target == null) {
            return -1;
        }
        try {
            Method m = cached;
            if (m == null) {
                m = findMethod(target.getClass(), name);
                if (m == null) {
                    return -1;
                }
                if ("getCurrentPosition".equals(name)) {
                    currentPositionMethod = m;
                } else if ("getDuration".equals(name)) {
                    durationMethod = m;
                }
            }
            Object r = m.invoke(target);
            return r instanceof Number ? ((Number) r).longValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static Method findMethod(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // 继续向父类查找
            }
        }
        return null;
    }

    private static String format(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.ROOT, "%02d:%02d", m, s);
    }

    private static int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                (appContext != null ? appContext.getResources()
                        : android.content.res.Resources.getSystem())
                        .getDisplayMetrics()));
    }
}
