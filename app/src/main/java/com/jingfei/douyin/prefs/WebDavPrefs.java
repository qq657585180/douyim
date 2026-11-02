package com.jingfei.douyin.prefs;

/**
 * WebDAV 备份（douyin_system）连接配置键。
 * 由抖X"系统数据 → WebDAV备份与恢复"移植。
 */
public final class WebDavPrefs {

    /** WebDAV 服务器地址（https://…，允许明文 HTTP 时可填 http://…）。 */
    public static final String KEY_SERVER = "webdav_server";

    /** 账号。 */
    public static final String KEY_USERNAME = "webdav_username";

    /** 密码 / 应用专用密码。 */
    public static final String KEY_PASSWORD = "webdav_password";

    /** 允许明文 HTTP（默认 false）。 */
    public static final String KEY_ALLOW_HTTP = "webdav_allow_http";

    /** 远端备份文件名（默认 douyin_config.json）。 */
    public static final String KEY_REMOTE_PATH = "webdav_remote_path";

    private WebDavPrefs() {
    }
}
