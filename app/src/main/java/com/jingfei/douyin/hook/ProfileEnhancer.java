package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Log;

import com.jingfei.douyin.prefs.ProfilePrefs;

/**
 * 个人主页（douyin_profile）Hook 子系统。已配置读取；默认作品页/作品视图/主页工具等 Hook 依赖抖音主页内部布局，暂无稳定 API 依据，保持 TODO。
 */
final class ProfileEnhancer {
    private static volatile SharedPreferences prefs;

    private ProfileEnhancer() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_profile");
        prefs = p;
        p.registerOnSharedPreferenceChangeListener(LISTENER);
        try {
            // 主页精确数量 + 作品网格精确点赞（profile.exact_counts / exact_grid_likes）
            ExactNumbers.setProfileEnabled(ProfilePrefs.getBoolean(
                    p, ProfilePrefs.KEY_PROFILE_EXACT_COUNTS, false));
            ExactNumbers.setGridLikesEnabled(ProfilePrefs.getBoolean(
                    p, ProfilePrefs.KEY_PROFILE_EXACT_GRID_LIKES, false));
            // TODO: 默认作品页/作品视图/主页工具 Hook
            module.log(Log.INFO, DouyinModule.TAG,
                    "ProfileEnhancer installed (douyin_profile)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "ProfileEnhancer skipped: " + error);
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
