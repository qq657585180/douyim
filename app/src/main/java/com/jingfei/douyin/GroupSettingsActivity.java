package com.jingfei.douyin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jingfei.douyin.hook.WebDavSync;
import com.jingfei.douyin.prefs.SettingMeta;

import java.util.List;
import java.util.Locale;

import io.github.libxposed.service.XposedService;

/**
 * 功能组设置页：根据 SettingMeta 渲染该组全部设置项。
 * 布尔项直接开关，数值/文本项输入后点保存。
 */
public final class GroupSettingsActivity extends Activity
        implements ModuleApplication.ServiceStateListener {

    private static final int BACKGROUND = Color.rgb(16, 17, 20);
    private static final int CARD = Color.rgb(28, 29, 34);
    private static final int PRIMARY = Color.rgb(254, 44, 85);
    private static final int TEXT_PRIMARY = Color.rgb(245, 245, 248);
    private static final int TEXT_SECONDARY = Color.rgb(169, 171, 180);

    private String group;
    private String prefName;
    private SharedPreferences preferences;
    private boolean loading = true;
    private LinearLayout listContainer;
    private XposedService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        group = intent.getStringExtra("group");
        if (group == null) {
            group = "system";
        }
        prefName = "douyin_" + group;
        setContentView(buildContent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        ModuleApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        ModuleApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged(XposedService service) {
        this.service = service;
        if (service == null) {
            preferences = null;
            setControlsEnabled(false);
            return;
        }
        preferences = service.getRemotePreferences(prefName);
        setControlsEnabled(true);
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root, matchWrap());

        TextView title = text(SettingMeta.groupLabel(group) + "设置", 24, TEXT_PRIMARY);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        List<SettingMeta> metas = SettingMeta.forGroup(group);
        TextView subtitle = text("共 " + metas.size() + " 项 · 设置自动同步给抖音进程", 13, TEXT_SECONDARY);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(6);
        root.addView(subtitle, subtitleParams);

        if ("system".equals(group)) {
            LinearLayout webdavRow = new LinearLayout(this);
            webdavRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams webdavRowParams = matchWrap();
            webdavRowParams.topMargin = dp(16);
            root.addView(webdavRow, webdavRowParams);

            Button backupButton = new Button(this);
            backupButton.setText("备份到 WebDAV");
            backupButton.setTextColor(Color.WHITE);
            backupButton.setTextSize(13);
            backupButton.setAllCaps(false);
            backupButton.setBackground(rounded(PRIMARY, 10));
            LinearLayout.LayoutParams backupParams = wrapWrap();
            backupParams.rightMargin = dp(6);
            backupParams.weight = 1;
            webdavRow.addView(backupButton, backupParams);

            Button restoreButton = new Button(this);
            restoreButton.setText("从 WebDAV 恢复");
            restoreButton.setTextColor(Color.WHITE);
            restoreButton.setTextSize(13);
            restoreButton.setAllCaps(false);
            restoreButton.setBackground(rounded(Color.rgb(60, 64, 75), 10));
            LinearLayout.LayoutParams restoreParams = wrapWrap();
            restoreParams.leftMargin = dp(6);
            restoreParams.weight = 1;
            webdavRow.addView(restoreButton, restoreParams);

            backupButton.setOnClickListener(v -> runWebDav(true));
            restoreButton.setOnClickListener(v -> runWebDav(false));
        }

        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(18);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer, listParams);

        for (SettingMeta meta : metas) {
            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.topMargin = dp(10);
            listContainer.addView(buildItemCard(meta), cardParams);
        }

        TextView footer = text(
                "设置保存在抖音进程的 SharedPreferences 中，模块 Hook 实时读取。",
                12,
                TEXT_SECONDARY
        );
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(18);
        root.addView(footer, footerParams);
        return scroll;
    }

    private View buildItemCard(final SettingMeta meta) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(CARD, 16));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(meta.title, 16, TEXT_PRIMARY);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        copy.addView(label, matchWrap());
        if (meta.description != null && !meta.description.isEmpty()) {
            TextView desc = text(meta.description, 12, TEXT_SECONDARY);
            desc.setLineSpacing(0, 1.2f);
            LinearLayout.LayoutParams descParams = matchWrap();
            descParams.topMargin = dp(3);
            copy.addView(desc, descParams);
        }
        header.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (meta.type == SettingMeta.TYPE_BOOLEAN) {
            Switch toggle = new Switch(this);
            toggle.setContentDescription(meta.title);
            toggle.setShowText(false);
            toggle.setButtonTintList(null);
            toggle.setOnCheckedChangeListener((button, checked) -> {
                if (!loading) {
                    saveBoolean(meta.key, checked);
                }
            });
            header.addView(toggle, wrapWrap());
            card.addView(header, matchWrap());
            return card;
        }

        // 数值 / 文本输入
        card.addView(header, matchWrap());

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams inputRowParams = matchWrap();
        inputRowParams.topMargin = dp(10);
        card.addView(inputRow, inputRowParams);

        final EditText input = new EditText(this);
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(Color.rgb(112, 114, 123));
        input.setTextSize(14);
        input.setSingleLine(true);
        if (meta.type == SettingMeta.TYPE_NUMBER) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        GradientDrawable inputBackground = rounded(Color.rgb(38, 39, 45), 10);
        inputBackground.setStroke(dp(1), Color.rgb(58, 60, 68));
        input.setBackground(inputBackground);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        inputRow.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button save = new Button(this);
        save.setText("保存");
        save.setTextColor(Color.WHITE);
        save.setTextSize(13);
        save.setAllCaps(false);
        save.setBackground(rounded(PRIMARY, 10));
        LinearLayout.LayoutParams saveParams = wrapWrap();
        saveParams.leftMargin = dp(10);
        inputRow.addView(save, saveParams);

        // 初始化当前值
        String current = "";
        if (meta.type == SettingMeta.TYPE_NUMBER) {
            current = String.valueOf(getIntValue(meta.key, 0));
        } else {
            current = getStringValue(meta.key, "");
        }
        input.setText(current);
        if (current.length() > 0) {
            input.setSelection(current.length());
        }
        save.setOnClickListener(v -> {
            if (preferences == null) {
                Toast.makeText(this, "未连接 LSPosed 服务", Toast.LENGTH_SHORT).show();
                return;
            }
            String raw = input.getText().toString().trim();
            if (meta.type == SettingMeta.TYPE_NUMBER) {
                int value;
                try {
                    value = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    value = 0;
                }
                preferences.edit().putInt(meta.key, value).apply();
            } else {
                preferences.edit().putString(meta.key, raw).apply();
            }
            Toast.makeText(this, "已保存：" + meta.title, Toast.LENGTH_SHORT).show();
        });
        return card;
    }

    private boolean getBooleanValue(String key, boolean def) {
        return preferences != null && preferences.getBoolean(key, def);
    }

    private int getIntValue(String key, int def) {
        return preferences == null ? def : preferences.getInt(key, def);
    }

    private String getStringValue(String key, String def) {
        return preferences == null ? def : preferences.getString(key, def);
    }

    private void saveBoolean(String key, boolean value) {
        SharedPreferences current = preferences;
        if (current != null) {
            current.edit().putBoolean(key, value).apply();
        }
    }

    private void runWebDav(boolean backup) {
        XposedService current = service;
        if (current == null) {
            Toast.makeText(this, "未连接 LSPosed 服务", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, backup ? "正在备份…" : "正在恢复…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                if (backup) {
                    WebDavSync.backupAll(current);
                } else {
                    WebDavSync.restoreAll(current);
                }
                runOnUiThread(() -> Toast.makeText(this,
                        backup ? "WebDAV 备份成功" : "WebDAV 恢复成功", Toast.LENGTH_SHORT).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        (backup ? "备份失败" : "恢复失败") + "：" + error.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void setControlsEnabled(boolean enabled) {
        // 设置项渲染后再统一启用；这里只控制整体透明度提示
        loading = false;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(2), dp(2), dp(2), dp(2));
        layout.setBackground(rounded(CARD, 16));
        return layout;
    }

    private TextView text(CharSequence value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
