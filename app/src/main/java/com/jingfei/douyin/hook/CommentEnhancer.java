package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.CommentPrefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 评论增强（douyin_comment）Hook 子系统。
 * 已实现：评论关键词过滤 / 屏蔽用户 / 白名单 / 广告评论过滤。
 * 通过 Activity 生命周期驱动主线程定时扫描评论区 RecyclerView，
 * 命中过滤规则的评论项直接隐藏。其余评论区高级 UI 依赖抖音内部视图结构，保持 TODO。
 */
final class CommentEnhancer {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = CommentEnhancer::scanComments;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;

    /** 评论区子项的时间/操作文本特征，用于区分评论项与视频项 */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "刚刚|分钟前|小时前|天前|昨天|回复|赞");

    /** 内置广告评论特征词 */
    private static final String[] AD_HINTS = {
            "加v", "加vx", "加微信", "vx:", "vx：", "微信", "qq群", "扣扣",
            "淘宝", "拼多多", "刷单", "兼职", "代购", "返利", "点赞关注",
            "关注我", "私信我", "主页有", "橱窗", "低价", "限时优惠"
    };

    private CommentEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_comment");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            module.log(Log.INFO, DouyinModule.TAG,
                    "CommentEnhancer installed (douyin_comment)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "CommentEnhancer skipped: " + error);
        }
    }

    /** 由 DouyinModule 的 Activity onResume 拦截调用 */
    static void onActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        currentActivity = new WeakReference<>(activity);
        if (!scanning) {
            scanning = true;
            HANDLER.postDelayed(SCAN, 500L);
        }
    }

    /** 由 DouyinModule 的 Activity onPause 拦截调用 */
    static void onActivityPaused(Activity activity) {
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
        }
    }

    private static void scanComments() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                scanView(decor);
            }
        } catch (Throwable ignored) {
            // 视图结构变化或权限异常时跳过本轮
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
    }

    private static void scanView(View view) {
        if (view == null) {
            return;
        }
        if (isRecyclerView(view)) {
            ViewGroup rv = (ViewGroup) view;
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                if (child == null || child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                if (isCommentItem(child)) {
                    filterCommentItem(child);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scanView(group.getChildAt(i));
            }
        }
    }

    private static boolean isRecyclerView(View view) {
        return view instanceof ViewGroup
                && view.getClass().getName().contains("RecyclerView");
    }

    /** 启发式判断评论项：至少两个文本控件，且含时间/回复/点赞特征 */
    private static boolean isCommentItem(View item) {
        List<String> texts = collectTexts(item);
        if (texts.size() < 2) {
            return false;
        }
        for (String text : texts) {
            if (text != null && TIME_PATTERN.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static void filterCommentItem(View item) {
        if (!bool(CommentPrefs.KEY_COMMENT_FILTER_5E6DB5D12C, false)) {
            return;
        }
        List<String> texts = collectTexts(item);
        if (texts.isEmpty()) {
            return;
        }
        String nickname = texts.get(0) == null ? "" : texts.get(0).trim();
        String body = join(texts);

        Set<String> whitelist = parseList(string(
                CommentPrefs.KEY_COMMENT_FILTER_6F055918E2, ""));
        if (whitelist.contains(nickname)) {
            return;
        }

        boolean hide = false;

        String keywords = string(CommentPrefs.KEY_COMMENT_FILTER_2ECFEAF382, "");
        for (String keyword : parseList(keywords)) {
            if (!keyword.isEmpty() && containsIgnoreCase(body, keyword)) {
                hide = true;
                break;
            }
        }

        if (!hide) {
            Set<String> blocked = parseList(string(
                    CommentPrefs.KEY_COMMENT_FILTER_C28BBE3819, ""));
            if (blocked.contains(nickname)) {
                hide = true;
            }
        }

        if (!hide && bool(CommentPrefs.KEY_COMMENT_FILTER_CD2238182A, false)) {
            for (String hint : AD_HINTS) {
                if (containsIgnoreCase(body, hint)) {
                    hide = true;
                    break;
                }
            }
        }

        if (hide) {
            item.setVisibility(View.GONE);
        }
    }

    private static List<String> collectTexts(View view) {
        List<String> texts = new ArrayList<>();
        collectTexts(view, texts);
        return texts;
    }

    private static void collectTexts(View view, List<String> texts) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null && cs.length() > 0) {
                texts.add(cs.toString());
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTexts(group.getChildAt(i), texts);
            }
        }
    }

    private static String join(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (String text : texts) {
            if (text != null) {
                sb.append(text).append('\n');
            }
        }
        return sb.toString();
    }

    private static Set<String> parseList(String raw) {
        Set<String> result = new HashSet<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String part : raw.split("[,，;；、\\n/|]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT));
    }

    static boolean bool(String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    static int integer(String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    static String string(String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER =
            (preferences, key) -> {
                // 配置变更后无需额外处理：扫描时实时读取
            };
}
