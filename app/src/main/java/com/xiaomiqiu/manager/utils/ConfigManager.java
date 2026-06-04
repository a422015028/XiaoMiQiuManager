package com.xiaomiqiu.manager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiaomiqiu.manager.model.Config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final String PREFS_NAME = "xiaomiqiu_configs";
    private static final String KEY_CONFIGS = "configs";
    private static final String KEY_DEFAULT_CONFIG_ID = "default_config_id";

    private final SharedPreferences prefs;
    private final Gson gson;

    public ConfigManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public List<Config> getAllConfigs() {
        String json = prefs.getString(KEY_CONFIGS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Config>>() {}.getType();
        List<Config> configs = gson.fromJson(json, type);
        return configs != null ? configs : new ArrayList<>();
    }

    public void saveConfig(Config config) {
        List<Config> configs = getAllConfigs();

        // Check if config already exists
        boolean found = false;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getId().equals(config.getId())) {
                configs.set(i, config);
                found = true;
                break;
            }
        }

        if (!found) {
            configs.add(config);
        }

        // If this is set as default, unset others
        if (config.isDefault()) {
            for (Config c : configs) {
                if (!c.getId().equals(config.getId())) {
                    c.setDefault(false);
                }
            }
        }

        saveConfigs(configs);
    }

    public void deleteConfig(String configId) {
        List<Config> configs = getAllConfigs();
        configs.removeIf(c -> c.getId().equals(configId));
        saveConfigs(configs);

        // If we deleted the default, clear default id
        String defaultId = prefs.getString(KEY_DEFAULT_CONFIG_ID, null);
        if (configId.equals(defaultId)) {
            prefs.edit().remove(KEY_DEFAULT_CONFIG_ID).apply();
        }
    }

    private void saveConfigs(List<Config> configs) {
        String json = gson.toJson(configs);
        prefs.edit().putString(KEY_CONFIGS, json).apply();
    }

    public Config getDefaultConfig() {
        String defaultId = prefs.getString(KEY_DEFAULT_CONFIG_ID, null);
        List<Config> configs = getAllConfigs();

        if (defaultId != null) {
            for (Config config : configs) {
                if (config.getId().equals(defaultId)) {
                    return config;
                }
            }
        }

        // Return first config marked as default, or first config
        for (Config config : configs) {
            if (config.isDefault()) {
                return config;
            }
        }

        return configs.isEmpty() ? null : configs.get(0);
    }

    public void setDefaultConfig(String configId) {
        prefs.edit().putString(KEY_DEFAULT_CONFIG_ID, configId).apply();

        List<Config> configs = getAllConfigs();
        for (Config config : configs) {
            config.setDefault(config.getId().equals(configId));
        }
        saveConfigs(configs);
    }

    public Config getConfigById(String configId) {
        List<Config> configs = getAllConfigs();
        for (Config config : configs) {
            if (config.getId().equals(configId)) {
                return config;
            }
        }
        return null;
    }
}
