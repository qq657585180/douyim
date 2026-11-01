package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * message 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class MessagePrefs {
    public static final String NAME = "douyin_message";

    /** 选择会话（streak） */
    public static final String KEY_STREAK = "streak";

    /** 隐藏消息页顶部朋友（message.friends） */
    public static final String KEY_MESSAGE_FRIENDS = "message.friends";

    /** 隐私与状态（privacy） */
    public static final String KEY_PRIVACY = "privacy";

    /** 消息长按工具（messages.3e3feb2a50） */
    public static final String KEY_MESSAGES_3E3FEB2A50 = "messages.3e3feb2a50";

    /** 右滑引用回复（messages.30ce80fcb1） */
    public static final String KEY_MESSAGES_30CE80FCB1 = "messages.30ce80fcb1";

    /** 聊天语音工具（messages.ad76394fb6） */
    public static final String KEY_MESSAGES_AD76394FB6 = "messages.ad76394fb6";

    /** 收藏表情管理（messages.0d6bc93404） */
    public static final String KEY_MESSAGES_0D6BC93404 = "messages.0d6bc93404";

    /** 消息一键已读（messages.5583c877cb） */
    public static final String KEY_MESSAGES_5583C877CB = "messages.5583c877cb";

    /** 导出聊天记录（messages.6e933e99e2） */
    public static final String KEY_MESSAGES_6E933E99E2 = "messages.6e933e99e2";

    /** 自动续火（messages.ff3e0ab8e6） */
    public static final String KEY_MESSAGES_FF3E0AB8E6 = "messages.ff3e0ab8e6";

    /** 阻止已读上报（privacy.9204803a27） */
    public static final String KEY_PRIVACY_9204803A27 = "privacy.9204803a27";

    /** 阻止正在输入上报（privacy.b9e6907634） */
    public static final String KEY_PRIVACY_B9E6907634 = "privacy.b9e6907634";

    /** 阻止聊天截图通知（privacy.ac66d7ae6c） */
    public static final String KEY_PRIVACY_AC66D7AE6C = "privacy.ac66d7ae6c";

    /** 显示本地撤回原文（privacy.c7ea651bf4） */
    public static final String KEY_PRIVACY_C7EA651BF4 = "privacy.c7ea651bf4";

    /** 阻止仅看一次销毁（privacy.dab943c438） */
    public static final String KEY_PRIVACY_DAB943C438 = "privacy.dab943c438";

    /** 开启仅看一次入口（privacy.49e47e3d4a） */
    public static final String KEY_PRIVACY_49E47E3D4A = "privacy.49e47e3d4a";

    /** 本地访客开启标识（privacy.97e76da8b0） */
    public static final String KEY_PRIVACY_97E76DA8B0 = "privacy.97e76da8b0";

    private MessagePrefs() {
    }

    public static boolean getBoolean(SharedPreferences prefs, String key, boolean def) {
        return prefs != null && prefs.getBoolean(key, def);
    }

    public static int getInt(SharedPreferences prefs, String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    public static String getString(SharedPreferences prefs, String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    public static void putBoolean(SharedPreferences prefs, String key, boolean value) {
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    public static void putInt(SharedPreferences prefs, String key, int value) {
        if (prefs != null) prefs.edit().putInt(key, value).apply();
    }

    public static void putString(SharedPreferences prefs, String key, String value) {
        if (prefs != null) prefs.edit().putString(key, value).apply();
    }
}