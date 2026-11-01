package com.jingfei.douyin.prefs;

import android.content.SharedPreferences;

/**
 * download 组设置项（由抖X加密串自动生成，字段名与抖X一致）。
 */
public final class DownloadPrefs {
    public static final String NAME = "douyin_download";

    /** 下载格式与目录（page.download） */
    public static final String KEY_PAGE_DOWNLOAD = "page.download";

    /** 长按菜单隐藏项（longpress_menu） */
    public static final String KEY_LONGPRESS_MENU = "longpress_menu";

    /** 长按菜单工具（longpress_tools） */
    public static final String KEY_LONGPRESS_TOOLS = "longpress_tools";

    /** 打开分享口令（token） */
    public static final String KEY_TOKEN = "token";

    /** 保存位置（paths.0b7e287692） */
    public static final String KEY_PATHS_0B7E287692 = "paths.0b7e287692";

    /** 改回系统相册／音乐（paths.1b6cfe60d5） */
    public static final String KEY_PATHS_1B6CFE60D5 = "paths.1b6cfe60d5";

    /** 路径模板（不含扩展名）（paths.d094b9e3ec） */
    public static final String KEY_PATHS_D094B9E3EC = "paths.d094b9e3ec";

    /** 日期格式（paths.7530fa1a19） */
    public static final String KEY_PATHS_7530FA1A19 = "paths.7530fa1a19";

    /** 抖音通知权限（download_interaction.b3668c01d7） */
    public static final String KEY_DOWNLOAD_INTERACTION_B3668C01D7 = "download_interaction.b3668c01d7";

    /** 保存前确认文件名（download_interaction.60b3a39899） */
    public static final String KEY_DOWNLOAD_INTERACTION_60B3A39899 = "download_interaction.60b3a39899";

    /** 下载窗口默认最小化（download_interaction.4512fa866c） */
    public static final String KEY_DOWNLOAD_INTERACTION_4512FA866C = "download_interaction.4512fa866c";

    /** 显示下载通知（download_interaction.2c25d425da） */
    public static final String KEY_DOWNLOAD_INTERACTION_2C25D425DA = "download_interaction.2c25d425da";

    /** 隐藏原生分享项目（share.native） */
    public static final String KEY_SHARE_NATIVE = "share.native";

    /** 隐藏原生好友列表（share.8db1c99607） */
    public static final String KEY_SHARE_8DB1C99607 = "share.8db1c99607";

    /** 隐藏私信分享成功提示（share.27d06f49fc） */
    public static final String KEY_SHARE_27D06F49FC = "share.27d06f49fc";

    /** 视频格式（download.e4e86807ea） */
    public static final String KEY_DOWNLOAD_E4E86807EA = "download.e4e86807ea";

    /** 保存子目录（download.3b44f4c721） */
    public static final String KEY_DOWNLOAD_3B44F4C721 = "download.3b44f4c721";

    private DownloadPrefs() {
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