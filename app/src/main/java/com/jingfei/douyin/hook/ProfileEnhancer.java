package com.jingfei.douyin.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jingfei.douyin.prefs.ProfilePrefs;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * 个人主页（douyin_profile）Hook 子系统。
 * 已实现：主页精确数量/作品网格精确点赞（→ ExactNumbers）；默认打开作品页（profile.7279f9e6d6）。
 * 待补充：作品视图/主页工具等依赖抖音主页内部布局，暂无稳定 API 依据。
 */
final class ProfileEnhancer {
    private static volatile SharedPreferences prefs;
    private static volatile WeakReference<Activity> currentActivity;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Runnable SCAN = ProfileEnhancer::scanProfileUi;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static volatile boolean scanning;
    private static long lastWorksTabClickAt;

    private ProfileEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_profile");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            // 页面总开关（page.profile）
            boolean pageEnabled = ProfilePrefs.getBoolean(p, ProfilePrefs.KEY_PAGE_PROFILE, true);
            // 主页精确数量 + 作品网格精确点赞（profile.exact_counts / exact_grid_likes）
            ExactNumbers.setProfileEnabled(pageEnabled && ProfilePrefs.getBoolean(
                    p, ProfilePrefs.KEY_PROFILE_EXACT_COUNTS, false));
            ExactNumbers.setGridLikesEnabled(pageEnabled && ProfilePrefs.getBoolean(
                    p, ProfilePrefs.KEY_PROFILE_EXACT_GRID_LIKES, false));
            module.log(Log.INFO, DouyinModule.TAG,
                    "ProfileEnhancer installed (douyin_profile)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "ProfileEnhancer skipped: " + error);
        }
    }

    /** Activity 可见后开始扫描主页（由 DouyinModule 生命周期 Hook 调用）。 */
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

    /** 主页 UI 增强：默认打开"作品"页。 */
    private static void scanProfileUi() {
        Activity activity = currentActivity == null ? null : currentActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            scanning = false;
            return;
        }
        // 页面总开关（page.profile）：关闭则主页增强整体不生效
        if (!bool(ProfilePrefs.KEY_PAGE_PROFILE, true)) {
            scanning = false;
            return;
        }
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                // 主页作品视图切换（profile_style）：单列/双列
                applyProfileStyle(decor);
                // 默认打开作品页（profile.7279f9e6d6）：若当前不在"作品"Tab 则点击
                if (bool(ProfilePrefs.KEY_PROFILE_7279F9E6D6, false)) {
                    long now = SystemClock.uptimeMillis();
                    if (now - lastWorksTabClickAt > 5_000L) {
                        View tab = findTabByText(decor, "作品");
                        if (tab != null && !tab.isSelected()) {
                            lastWorksTabClickAt = now;
                            try {
                                tab.performClick();
                            } catch (Throwable ignored) {
                                // 点击失败不影响主流程
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

    /** 查找文本命中的 Tab：TextView 向上找可点击/已选中的容器。 */
    private static View findTabByText(View view, String text) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            String s = cs == null ? "" : cs.toString();
            if (s.contains(text)) {
                View cur = view;
                for (int i = 0; i < 4 && cur != null; i++) {
                    if (cur.isClickable() || cur.isSelected()) {
                        return cur;
                    }
                    if (cur.getParent() instanceof ViewGroup) {
                        cur = (ViewGroup) cur.getParent();
                    } else {
                        break;
                    }
                }
                return view;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTabByText(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 主页作品视图切换（profile_style）：把作品网格在单列/双列间切换。
     *  读取字符串值：含"单列"/single 或为 "1" 时切单列，否则恢复双列。 */
    private static void applyProfileStyle(View decor) {
        String style = string(ProfilePrefs.KEY_PROFILE_STYLE, "");
        if (style == null || style.length() == 0) {
            return;
        }
        boolean single;
        String trimmed = style.trim();
        if (trimmed.contains("单列") || trimmed.contains("single")
                || "1".equals(trimmed)) {
            single = true;
        } else {
            single = false;
        }
        setGridSpan(decor, single ? 1 : 2);
    }

    /** 递归把 GridLayoutManager 的 spanCount 改为目标值（幂等，只在实际不同时修改）。 */
    private static void setGridSpan(View view, int span) {
        if (view == null) {
            return;
        }
        try {
            if (view.getClass().getName().contains("RecyclerView")) {
                Method getLm = view.getClass().getMethod("getLayoutManager");
                Object lm = getLm.invoke(view);
                if (lm != null && lm.getClass().getName()
                        .contains("GridLayoutManager")) {
                    Method getSpan = lm.getClass().getMethod("getSpanCount");
                    int current = (Integer) getSpan.invoke(lm);
                    if (current != span) {
                        Method setSpan = lm.getClass()
                                .getMethod("setSpanCount", int.class);
                        setSpan.invoke(lm, span);
                    }
                }
            }
        } catch (Throwable ignored) {
            // 布局类不存在或方法变化时跳过
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setGridSpan(group.getChildAt(i), span);
            }
        }
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
                // 配置变更后无需额外处理：Hook 实时读取 getBoolean/getInt/getString
            };
}
