package com.jingfei.douyin.hook;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 弹幕增强（抖X danmaku.*）。
 * 已实现：屏蔽关键词（danmaku.1d6c3a1841）、弹幕颜色（danmaku.9daa16e0ab）。
 * 由 ImmersiveUi 播放页扫描循环驱动，对识别到的弹幕视图实时应用过滤与颜色。
 */
final class DanmakuEnhancer {
    private static volatile boolean filterEnabled;
    private static volatile boolean dedupeEnabled;
    private static volatile Pattern keywordPattern;
    private static volatile int danmakuColor = 0;
    /** 最近出现的弹幕文本（用于重复弹幕去重）。 */
    private static final java.util.LinkedHashSet<String> SEEN =
            new java.util.LinkedHashSet<>();
    private static final int MAX_SEEN = 200;

    private DanmakuEnhancer() {
    }

    /** 由 PlaybackStrategy 注入弹幕设置。 */
    static void applyPrefs(boolean enabled, String keywords, int color,
                           boolean dedupe) {
        filterEnabled = enabled;
        dedupeEnabled = dedupe;
        if (keywords != null && !keywords.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] parts = keywords.split("[,，;；、\n/|]");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(Pattern.quote(trimmed));
            }
            keywordPattern = sb.length() > 0 ? Pattern.compile(sb.toString()) : null;
        } else {
            keywordPattern = null;
        }
        danmakuColor = color;
    }

    /** 扫描视图树，对弹幕视图应用关键词过滤与颜色。 */
    static void update(View decor) {
        if (decor == null) {
            return;
        }
        if (!filterEnabled && danmakuColor == 0) {
            return;
        }
        scan(decor);
    }

    private static void scan(View view) {
        if (view == null) {
            return;
        }
        if (isDanmakuView(view)) {
            apply((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scan(group.getChildAt(i));
            }
        }
    }

    /** 启发式识别弹幕视图：TextView 且类名/父类名含弹幕特征，或当前运行位移动画。 */
    private static boolean isDanmakuView(View view) {
        if (!(view instanceof TextView)) {
            return false;
        }
        String name = view.getClass().getName().toLowerCase(Locale.ROOT);
        if (name.contains("danmaku") || name.contains("barrage")
                || name.contains("bullet")) {
            return true;
        }
        View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
        if (parent != null) {
            String parentName = parent.getClass().getName().toLowerCase(Locale.ROOT);
            if (parentName.contains("danmaku") || parentName.contains("barrage")
                    || parentName.contains("bullet")) {
                return true;
            }
        }
        // 兜底：文本较短、处于位移动画中的浮层文本视为弹幕
        return view.getAnimation() != null
                && view.getAnimation().getClass().getName().toLowerCase(Locale.ROOT)
                .contains("translate");
    }

    private static void apply(TextView tv) {
        if (tv.getVisibility() != View.VISIBLE) {
            return;
        }
        CharSequence cs = tv.getText();
        if (cs == null) {
            return;
        }
        String text = cs.toString();
        if (filterEnabled && keywordPattern != null
                && keywordPattern.matcher(text).find()) {
            tv.setVisibility(View.GONE);
            return;
        }
        if (dedupeEnabled) {
            if (SEEN.contains(text)) {
                tv.setVisibility(View.GONE);
                return;
            }
            SEEN.add(text);
            while (SEEN.size() > MAX_SEEN) {
                java.util.Iterator<String> it = SEEN.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }
        if (danmakuColor != 0) {
            tv.setTextColor(danmakuColor);
        }
    }
}
