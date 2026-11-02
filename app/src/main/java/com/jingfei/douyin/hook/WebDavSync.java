package com.jingfei.douyin.hook;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.jingfei.douyin.prefs.WebDavPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.service.XposedService;

/**
 * WebDAV 备份与恢复（douyin_system）。
 * 由抖X"系统数据 → WebDAV 备份与恢复"移植：
 * 将全部模块配置（douyin_* SharedPreferences）序列化为 JSON，
 * 通过 WebDAV PUT 上传 / GET 下载，实现跨设备配置备份与恢复。
 */
public final class WebDavSync {
    private static volatile SharedPreferences prefs;

    /** 参与备份的全部模块配置名。 */
    private static final String[] MODULE_PREFS = {
            "douyin_playback",
            "douyin_comment",
            "douyin_message",
            "douyin_purify",
            "douyin_appearance",
            "douyin_download",
            "douyin_live",
            "douyin_profile",
            "douyin_system"
    };

    private WebDavSync() {
    }

    static void install(DouyinModule module, ClassLoader loader) {
        SharedPreferences p = module.getRemotePreferences("douyin_system");
        prefs = p;
        if (p != null) {
            p.registerOnSharedPreferenceChangeListener(LISTENER);
        }
        try {
            module.log(Log.INFO, DouyinModule.TAG,
                    "WebDavSync installed (douyin_system)");
        } catch (Throwable error) {
            module.log(Log.WARN, DouyinModule.TAG,
                    "WebDavSync skipped: " + error);
        }
    }

    /** 将所有模块配置备份到 WebDAV。 */
    public static void backupAll(XposedService service) throws Exception {
        String url = buildTargetUrl(service);
        JSONObject root = new JSONObject();
        root.put("app", "douyim");
        root.put("version", 1);
        JSONObject prefsJson = new JSONObject();
        for (String name : MODULE_PREFS) {
            prefsJson.put(name, dumpPrefs(service.getRemotePreferences(name)));
        }
        root.put("prefs", prefsJson);

        HttpURLConnection connection = open(url, service);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        byte[] body = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        int code = connection.getResponseCode();
        connection.disconnect();
        if (code >= 400) {
            throw new IllegalStateException("备份失败：HTTP " + code);
        }
    }

    /** 从 WebDAV 下载并恢复全部模块配置。 */
    public static void restoreAll(XposedService service) throws Exception {
        String url = buildTargetUrl(service);
        HttpURLConnection connection = open(url, service);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        if (code >= 400) {
            connection.disconnect();
            throw new IllegalStateException("恢复失败：HTTP " + code
                    + "（远端可能不存在备份文件）");
        }
        String body;
        try (InputStream input = connection.getInputStream()) {
            body = readAll(input);
        }
        connection.disconnect();

        JSONObject root = new JSONObject(body);
        JSONObject prefsJson = root.optJSONObject("prefs");
        if (prefsJson == null) {
            throw new IllegalStateException("备份文件格式不正确");
        }
        for (String name : MODULE_PREFS) {
            JSONObject data = prefsJson.optJSONObject(name);
            if (data == null) {
                continue;
            }
            SharedPreferences.Editor editor = service.getRemotePreferences(name).edit();
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = data.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Double) {
                    editor.putFloat(key, (float) (double) (Double) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof JSONArray) {
                    // 集合类型以 JSONArray 存储，恢复为字符串集合。
                    JSONArray array = (JSONArray) value;
                    Set<String> set = new java.util.LinkedHashSet<>();
                    for (int i = 0; i < array.length(); i++) {
                        set.add(String.valueOf(array.get(i)));
                    }
                    editor.putStringSet(key, set);
                }
            }
            editor.apply();
        }
    }

    /** 计算远端完整 URL（服务器 + 路径）。 */
    private static String buildTargetUrl(XposedService service) {
        SharedPreferences sys = service.getRemotePreferences("douyin_system");
        String server = trim(sys.getString(WebDavPrefs.KEY_SERVER, ""));
        String path = trim(sys.getString(WebDavPrefs.KEY_REMOTE_PATH, "douyin_config.json"));
        boolean allowHttp = sys.getBoolean(WebDavPrefs.KEY_ALLOW_HTTP, false);
        if (server.isEmpty()) {
            throw new IllegalStateException("请先在“系统数据”中配置 WebDAV 服务器地址");
        }
        if (!server.startsWith("https://")
                && !(allowHttp && server.startsWith("http://"))) {
            throw new IllegalStateException("地址需以 https:// 开头，或开启“允许明文 HTTP”");
        }
        return server.endsWith("/") ? server + path : server + "/" + path;
    }

    /** 打开带 Basic 认证的连接。 */
    private static HttpURLConnection open(String url, XposedService service) throws Exception {
        SharedPreferences sys = service.getRemotePreferences("douyin_system");
        String user = trim(sys.getString(WebDavPrefs.KEY_USERNAME, ""));
        String pass = sys.getString(WebDavPrefs.KEY_PASSWORD, "");
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        String token = user + ":" + pass;
        connection.setRequestProperty("Authorization",
                "Basic " + Base64.encodeToString(
                        token.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        return connection;
    }

    /** 将 SharedPreferences 全部键值序列化为 JSONObject。 */
    private static JSONObject dumpPrefs(SharedPreferences preferences) throws Exception {
        JSONObject object = new JSONObject();
        Map<String, ?> all = preferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                object.put(entry.getKey(), (Boolean) value);
            } else if (value instanceof Integer) {
                object.put(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                object.put(entry.getKey(), (Long) value);
            } else if (value instanceof Float) {
                object.put(entry.getKey(), (Float) value);
            } else if (value instanceof String) {
                object.put(entry.getKey(), (String) value);
            } else if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                object.put(entry.getKey(), array);
            }
        }
        return object;
    }

    private static String readAll(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
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
