package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.LivePrefs;

import java.lang.ref.WeakReference;

/**
 * 直播增强（douyin_live）Hook 子系统。
 * 已实现：直播具体人数（live.0d1ad71357）→ ExactNumbers；直播清爽（live.7420f518ca）隐藏礼物入口。
 * 待补充：默认画质/直播信息入口/移动网络后台续播/语音替换等依赖抖音直播内部视图，暂无稳定 API 依据。
 */
final class LiveEnhancer {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = LiveEnhancer::scanLiveUi;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;

    private LiveEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_live");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            // 页面总开关（page.live）
            boolean pageEnabled = LivePrefs.getBoolean(p, LivePrefs.KEY_PAGE_LIVE, true);
            // 直播具体人数（live.0d1ad71357）→ ExactNumbers
            ExactNumbers.setLiveEnabled(pageEnabled && LivePrefs.getBoolean(
                    p, LivePrefs.KEY_LIVE_0D1AD71357, false));
            module.log(Log.INFO, DouyinModule.TAG,
                    "LiveEnhancer installed (douyin_live)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "LiveEnhancer skipped: " + error);
        }
    }

    /** Activity 可见后开始扫描直播页（由 DouyinModule 生命周期 Hook 调用）。 */
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

    /** 直播页 UI 增强：清爽模式隐藏礼物入口等。 */
    private static void scanLiveUi() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        // 页面总开关（page.live）：关闭则直播增强整体不生效
        if (!bool(LivePrefs.KEY_PAGE_LIVE, true)) {
            PlayerHooks.setLivePlaybackQuality(null);
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                // 直播清爽（live.7420f518ca）：隐藏礼物入口
                if (bool(LivePrefs.KEY_LIVE_7420F518CA, false)) {
                    hideByText(decor, "礼物", 1, true);
                }
                // 直播默认画质（live.41fb1b555b）：注入播放器画质（若直播复用视频引擎则生效）
                String quality = string(LivePrefs.KEY_LIVE_41FB1B555B, null);
                if (quality != null && !quality.isEmpty()) {
                    PlayerHooks.setLivePlaybackQuality(quality);
                }
                // 直播信息入口（live.964ffe7b61）/ 当前直播信息（live.0e64c35bc2）：隐藏人数/在看信息条
                if (bool(LivePrefs.KEY_LIVE_964FFE7B61, false)
                        || bool(LivePrefs.KEY_LIVE_0E64C35BC2, false)) {
                    hideByText(decor, "人观看", 2, true);
                    hideByText(decor, "在看", 2, true);
                    hideByText(decor, "在线", 2, true);
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
