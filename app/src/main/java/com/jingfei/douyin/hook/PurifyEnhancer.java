package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

import com.jingfei.douyin.FilterPreferences;
import com.jingfei.douyin.prefs.PurifyPrefs;

/**
 * 净化过滤（douyin_purify）Hook 子系统。
 * 由抖X移植：将抖X净化设置桥接到 FeedContentTracker 的过滤逻辑。
 * 当 feed_filter 开启时，用抖X的广告跳过/关键词覆盖旧版过滤；关闭则回退旧版设置。
 */
final class PurifyEnhancer {
    private static volatile SharedPreferences prefs;

    private PurifyEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_purify");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            applyOverride();
            module.log(Log.INFO, DouyinModule.TAG,
                    "PurifyEnhancer installed (douyin_purify)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "PurifyEnhancer skipped: " + error);
        }
    }

    private static void applyOverride() {
        if (prefs == null) {
            return;
        }
        boolean feedFilterEnabled =
                PurifyPrefs.getBoolean(prefs, PurifyPrefs.KEY_FEED_FILTER, false)
                        && PurifyPrefs.getBoolean(
                        prefs, PurifyPrefs.KEY_FEED_FILTER_F42350811B, true);
        if (!feedFilterEnabled) {
            FeedContentTracker.setMinDiggCount(0);
            FeedContentTracker.setDurationBounds(0, 0);
            FeedContentTracker.setStatThresholds(0, 0, 0);
            FeedContentTracker.setMaxAgeDays(0);
            FeedContentTracker.applyPurifyOverride(null);
            return;
        }
        boolean skipAds =
                PurifyPrefs.getBoolean(prefs, PurifyPrefs.KEY_SKIPAD, true)
                        || PurifyPrefs.getBoolean(prefs, PurifyPrefs.KEY_PLAYLET_AD, true);
        String keywords = PurifyPrefs.getString(
                prefs, PurifyPrefs.KEY_FEED_FILTER_DF653A2BF7, "");
        long minDigg = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_A2065CAC44, 0);
        long minDuration = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_MIN_DURATION, 0);
        long maxDuration = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_MAX_DURATION, 0);
        long minComment = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_D2FC778560, 0);
        long minCollect = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_6AE0300933, 0);
        long minShare = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_E885E7E1B6, 0);
        int maxAgeDays = PurifyPrefs.getInt(
                prefs, PurifyPrefs.KEY_FEED_THRESHOLDS_61B4880C99, 0);
        FeedContentTracker.setMinDiggCount(minDigg);
        FeedContentTracker.setDurationBounds(minDuration, maxDuration);
        FeedContentTracker.setStatThresholds(minComment, minCollect, minShare);
        FeedContentTracker.setMaxAgeDays(maxAgeDays);
        FeedContentTracker.applyPurifyOverride(
                new FilterPreferences.Values(skipAds, false, false, false, keywords));
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
                prefs = preferences;
                try {
                    applyOverride();
                } catch (RuntimeException ignored) {
                    // 桥接失败不影响主流程
                }
            };
}
