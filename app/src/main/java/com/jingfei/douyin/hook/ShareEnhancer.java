package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.DownloadPrefs;

import java.lang.ref.WeakReference;

/**
 * 分享面板增强（share 组）。
 * 检测分享面板（含"复制链接/分享到/保存本地/转发"文本的容器）并隐藏原生项：
 * 原生分享项目（share.native）、好友列表（share.8db1c99607）、私信分享成功提示（share.27d06f49fc）。
 */
final class ShareEnhancer {
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static boolean scanning;
    private static final Runnable SCAN = ShareEnhancer::scanShareUi;

    private static final String[] SHARE_HINTS = {"复制链接", "分享到", "保存本地", "转发"};
    private static final String[] NATIVE_ITEMS = {"转发到日常", "分享项目"};
    private static final String[] SUCCESS_HINTS = {"已发送", "发送成功", "分享成功"};

    private ShareEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        prefs = module.getRemotePreferences(DownloadPrefs.NAME);
        module.log(Log.INFO, DouyinModule.TAG,
                "ShareEnhancer installed (douyin_download/share)");
    }

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

    static void onActivityPaused(Activity activity) {
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
        }
    }

    private static void scanShareUi() {
        try {
            Activity activity = currentActivity == null ? null : currentActivity.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                View decor = activity.getWindow().getDecorView();
                if (decor != null) {
                    View panel = findSharePanel(decor);
                    if (panel != null) {
                        if (bool(DownloadPrefs.KEY_SHARE_NATIVE, false)) {
                            for (String item : NATIVE_ITEMS) {
                                hideByText(panel, item, 2, true);
                            }
                        }
                        if (bool(DownloadPrefs.KEY_SHARE_8DB1C99607, false)) {
                            hideByText(panel, "朋友", 3, true);
                            hideByText(panel, "最近", 3, true);
                        }
                        if (bool(DownloadPrefs.KEY_SHARE_27D06F49FC, false)) {
                            for (String hint : SUCCESS_HINTS) {
                                hideByText(panel, hint, 2, true);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // 视图结构变化或权限异常时跳过本轮
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
    }

    /** 启发式定位分享面板：含分享特征文本的容器。 */
    private static View findSharePanel(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (group.getChildCount() > 0 && containsAnyHint(group)) {
                return group;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findSharePanel(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean containsAnyHint(View view) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String text = cs.toString();
                for (String hint : SHARE_HINTS) {
                    if (text.contains(hint)) {
                        return true;
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsAnyHint(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean bool(String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    private static boolean hideByText(View root, String target, int levels, boolean contains) {
        if (root instanceof TextView) {
            CharSequence cs = ((TextView) root).getText();
            String text = cs == null ? "" : cs.toString().trim();
            boolean hit = contains ? text.contains(target) : text.equals(target);
            if (hit) {
                View cur = root;
                for (int i = 0; i < levels && cur.getParent() instanceof ViewGroup; i++) {
                    cur = (ViewGroup) cur.getParent();
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
}
