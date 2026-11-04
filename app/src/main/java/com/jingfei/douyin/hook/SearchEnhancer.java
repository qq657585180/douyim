package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.SearchPrefs;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * 搜索页 / 设置页增强（search 组）。
 */
final class SearchEnhancer {
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static boolean scanning;
    private static final Runnable SCAN = SearchEnhancer::scanSearchUi;

    private SearchEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        prefs = module.getRemotePreferences(SearchPrefs.NAME);
        module.log(Log.INFO, DouyinModule.TAG, "SearchEnhancer installed (douyin_search)");
    }

    static void onActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        String name = activity.getClass().getName();
        if (name == null || !isTarget(name)) {
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

    private static boolean isTarget(String className) {
        String n = className.toLowerCase(Locale.ROOT);
        return n.contains("search") || n.contains("setting");
    }

    private static void scanSearchUi() {
        try {
            Activity activity = currentActivity == null ? null : currentActivity.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                View decor = activity.getWindow().getDecorView();
                if (decor != null) {
                    String name = activity.getClass().getName().toLowerCase(Locale.ROOT);
                    if (name.contains("search") && bool(SearchPrefs.KEY_SEARCH_5CE1E5947F, false)) {
                        hideByText(decor, "AI抖音", 2, true);
                    }
                    if (name.contains("setting") && bool(SearchPrefs.KEY_SEARCH_2C5104DF81, false)) {
                        hideByText(decor, "AI搜索", 2, true);
                    }
                }
            }
        } catch (Throwable ignored) {
            // 扫描失败不影响主流程
        }
        HANDLER.postDelayed(SCAN, SCAN_INTERVAL_MS);
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
