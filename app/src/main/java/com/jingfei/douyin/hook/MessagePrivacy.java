package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.MessagePrefs;

import java.lang.ref.WeakReference;

/**
 * 消息隐私（douyin_message）Hook 子系统。
 * 已实现：隐藏消息页顶部"朋友"入口（message.friends）。
 * 待补充：聊天工具/已读输入状态/撤回续火/聊天导出等依赖抖音内部消息协议，暂无稳定 API 依据。
 */
final class MessagePrivacy {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = MessagePrivacy::scanMessageUi;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;

    private MessagePrivacy() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_message");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            module.log(Log.INFO, DouyinModule.TAG,
                    "MessagePrivacy installed (douyin_message)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "MessagePrivacy skipped: " + error);
        }
    }

    /** Activity 可见后开始扫描消息页（由 DouyinModule 生命周期 Hook 调用）。 */
    static void onActivityResumed(Activity activity) {
        currentActivity = new WeakReference<>(activity);
        if (!scanning) {
            scanning = true;
            HANDLER.post(SCAN);
        }
    }

    static void onActivityPaused(Activity activity) {
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
        }
    }

    /** 消息页 UI 增强：隐藏顶部"朋友"入口等。 */
    private static void scanMessageUi() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                // 隐藏消息页顶部"朋友"标签（message.friends）
                if (bool(MessagePrefs.KEY_MESSAGE_FRIENDS, false)) {
                    hideByText(decor, "朋友", 1, true);
                }
            }
        } catch (Throwable ignored) {
            // 视图结构变化或权限异常时跳过本轮
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
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

    /** 按文本隐藏视图：找到文本命中项后向上提升 levels 层隐藏容器 */
    private static boolean hideByText(View root, String target, int levels, boolean contains) {
        if (root instanceof TextView) {
            CharSequence cs = ((TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            boolean hit = contains ? text.contains(target) : text.equals(target);
            if (hit) {
                View cur = root;
                for (int i = 0; i < levels && cur.getParent() instanceof ViewGroup; i++) {
                    ViewGroup parent = (ViewGroup) cur.getParent();
                    if (parent == null) {
                        return true;
                    }
                    cur = parent;
                }
                if (cur != root && cur.getVisibility() == View.VISIBLE) {
                    cur.setVisibility(View.GONE);
                    return true;
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hideByText(group.getChildAt(i), target, levels, contains)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER =
            (preferences, key) -> {
                // 配置变更后无需额外处理：Hook 实时读取 getBoolean/getInt/getString
            };
}
