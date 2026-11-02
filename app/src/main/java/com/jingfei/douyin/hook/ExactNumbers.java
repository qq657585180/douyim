package com.jingfei.douyin.hook;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖X"显示准确数量"系列功能：把 "1.2万 / 3.5亿" 缩写展开为精确数字。
 * <p>
 * 对应设置项：
 * - controls.9aa386bb74 右侧显示真实数量（视频右侧栏点赞/评论/收藏/分享）
 * - comments.70151eae18 显示准确点赞数（评论区点赞）
 * - live.0d1ad71357 直播具体人数
 * - profile.exact_counts 主页精确数量
 * - profile.exact_grid_likes 作品网格精确点赞
 */
final class ExactNumbers {
    private static volatile boolean rightEnabled;
    private static volatile boolean commentEnabled;
    private static volatile boolean liveEnabled;
    private static volatile boolean profileEnabled;
    private static volatile boolean gridLikesEnabled;

    private static final Pattern NUM_UNIT =
            Pattern.compile("(\\d+(?:\\.\\d+)?)([万亿])");

    private ExactNumbers() {
    }

    /** 注入全部开关。 */
    static void applyPrefs(boolean right, boolean comment, boolean live,
                           boolean profile, boolean gridLikes) {
        rightEnabled = right;
        commentEnabled = comment;
        liveEnabled = live;
        profileEnabled = profile;
        gridLikesEnabled = gridLikes;
    }

    /** 右侧显示真实数量（controls.9aa386bb74，由 douyin_appearance 组注入）。 */
    static void setRightEnabled(boolean enabled) {
        rightEnabled = enabled;
    }

    /** 显示准确点赞数（comments.70151eae18，由 douyin_comment 组注入）。 */
    static void setCommentEnabled(boolean enabled) {
        commentEnabled = enabled;
    }

    /** 直播具体人数（live.0d1ad71357，由 douyin_live 组注入）。 */
    static void setLiveEnabled(boolean enabled) {
        liveEnabled = enabled;
    }

    /** 主页精确数量（profile.exact_counts，由 douyin_profile 组注入）。 */
    static void setProfileEnabled(boolean enabled) {
        profileEnabled = enabled;
    }

    /** 作品网格精确点赞（profile.exact_grid_likes，由 douyin_profile 组注入）。 */
    static void setGridLikesEnabled(boolean enabled) {
        gridLikesEnabled = enabled;
    }

    /** 扫描整个视图树（右侧栏 / 直播 / 主页 / 网格点赞）。 */
    static void scan(View root) {
        if (root == null) {
            return;
        }
        if (!(rightEnabled || liveEnabled || profileEnabled || gridLikesEnabled)) {
            return;
        }
        try {
            scanRecursive(root);
        } catch (Throwable ignored) {
            // 视图结构变化时忽略
        }
    }

    /** 只扫描评论面板（评论准确点赞）。 */
    static void scanComments(View panel) {
        if (panel == null || !commentEnabled) {
            return;
        }
        try {
            scanRecursive(panel);
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    private static void scanRecursive(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            expand((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scanRecursive(group.getChildAt(i));
            }
        }
    }

    /** 把文本中的 "x万 / x亿" 展开为精确整数。 */
    private static void expand(TextView tv) {
        CharSequence cs = tv.getText();
        if (cs == null) {
            return;
        }
        String text = cs.toString();
        Matcher m = NUM_UNIT.matcher(text);
        if (!m.find()) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        do {
            double value;
            try {
                value = Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                return;
            }
            long multiplier = "亿".equals(m.group(2)) ? 100_000_000L : 10_000L;
            long exact = Math.round(value * multiplier);
            m.appendReplacement(sb, String.valueOf(exact));
        } while (m.find());
        m.appendTail(sb);
        tv.setText(sb.toString());
    }
}
