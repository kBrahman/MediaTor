
package z.zer.tor.media.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.util.JsonUtils;
import z.zer.tor.media.util.Logger;

import java.util.Map;
import java.util.Map.Entry;

@SuppressLint("CommitPrefEdits") // this is due to a lint false positive
public final class ConfigurationManager {

    private static final Logger LOG = Logger.getLogger(ConfigurationManager.class);

    private final SharedPreferences preferences;
    private final ConfigurationDefaults defaults;

    private static final Object creationLock = new Object();
    private static ConfigurationManager instance;

    public static void create(@NonNull Context context) {
        if (instance != null) {
            throw new RuntimeException("CHECK YOUR LOGIC: ConfigurationManager.create(ctx) can only be called once.");
        }
        Engine.instance().getThreadPool().execute(() -> {
            instance = new ConfigurationManager(context.getApplicationContext());
            synchronized (creationLock) {
                creationLock.notifyAll();
            }
        });
    }

    public static ConfigurationManager instance() {
        if (instance == null) {
            try {
                synchronized (creationLock) {
                    creationLock.wait(20000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (instance == null) {
            throw new RuntimeException("The ConfigurationManager instance() creation timed out, try reinstalling the app or notify FrostWire developers");
        }
        return instance;
    }

    private ConfigurationManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaults = new ConfigurationDefaults();
        initPreferences(preferences);
    }

    public String getString(String key, String defValue) {
        if (preferences == null) {
            LOG.error("getString(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getString(key=" + key + ") failed, preferences:SharedPreferences is null");
        }
        return preferences.getString(key, defValue);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public void setString(String key, String value) {
        try {
            setString(preferences.edit(), key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setString(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public int getInt(String key, int defValue) {
        if (preferences == null) {
            LOG.error("getInt(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getInt(key=" + key + ") failed, preferences:SharedPreferences is null");
        }
        return preferences.getInt(key, defValue);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public void setInt(String key, int value) {
        try {
            setInt(preferences.edit(), key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setInt(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public long getLong(String key, long defValue) {
        if (preferences == null) {
            LOG.error("getLong(key=" + key + ", defValue=" + defValue + ") preferences == null");
            throw new IllegalStateException("getLong(key=" + key + ") failed, preferences:SharedPreferences is null");
        }
        return preferences.getLong(key, defValue);
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public void setLong(String key, long value) {
        try {
            setLong(preferences.edit(), key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setLong(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public boolean getBoolean(String key) {
        if (preferences == null) {
            LOG.error("getBoolean(key=" + key + ") preferences == null");
            throw new IllegalStateException("getBoolean(key=" + key + ") failed, preferences:SharedPreferences is null");
        }
        return preferences.getBoolean(key, false);
    }

    public void setBoolean(String key, boolean value) {
        try {
            setBoolean(preferences.edit(), key, value).apply();
        } catch (Throwable ignore) {
            LOG.warn("setBoolean(key=" + key + ", value=" + value + ") failed", ignore);
        }
    }

    public void resetToDefaults() {
        resetToDefaults(preferences.edit(), defaults.getDefaultValues()).apply();
    }

    public int getLastMediaTypeFilter() {
        return getInt(Constants.PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER);
    }

    public void setLastMediaTypeFilter(int mediaTypeId) {
        setInt(Constants.PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER, mediaTypeId);
    }

    public boolean vibrateOnFinishedDownload() {
        return getBoolean(Constants.PREF_KEY_GUI_VIBRATE_ON_FINISHED_DOWNLOAD);
    }

    public boolean showTransfersOnDownloadStart() {
        return getBoolean(Constants.PREF_KEY_GUI_SHOW_TRANSFERS_ON_DOWNLOAD_START);
    }

    public void registerOnPreferenceChange(OnSharedPreferenceChangeListener listener) {
        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public void unregisterOnPreferenceChange(OnSharedPreferenceChangeListener listener) {
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    public String getStoragePath() {
        return getString(Constants.PREF_KEY_STORAGE_PATH);
    }

    public void setStoragePath(String path) {
        if (path != null && path.length() > 0) { // minor verifications
            setString(Constants.PREF_KEY_STORAGE_PATH, path);
        }
    }

    public boolean isSeedFinishedTorrents() {
        return getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
    }

    public void setSeedFinishedTorrents(boolean value) {
        setBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS, value);
    }

    public boolean isSeedingEnabledOnlyForWifi() {
        return getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
    }

    private void initPreferences(@NonNull SharedPreferences preferences) {
        Editor editor = preferences.edit();

        for (Entry<String, Object> entry : defaults.getDefaultValues().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!preferences.contains(key)) {
                setPreference(editor, key, value);
            }
        }

        //there are some configuration values that need to be reset every time to a desired value
        resetToDefaults(editor, defaults.getResetValues()).apply();
    }

    private Editor resetToDefaults(@NonNull Editor editor, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            setPreference(editor, entry.getKey(), entry.getValue());
        }

        return editor;
    }

    private void setPreference(@NonNull Editor editor, String key, Object value) {
        if (value instanceof String) {
            setString(editor, key, (String) value);
        } else if (value instanceof Integer) {
            setInt(editor, key, (Integer) value);
        } else if (value instanceof Long) {
            setLong(editor, key, (Long) value);
        } else if (value instanceof Boolean) {
            setBoolean(editor, key, (Boolean) value);
        } else if (value instanceof String[]) {
            setStringArray(editor, key, (String[]) value);
        } else {
            throw new RuntimeException("Unsupported data type for setting: " +
                    "key = " + key + ", value = " + (value != null ? value.getClass() : "null"));
        }
    }

    private Editor setString(Editor editor, String key, String value) {
        return editor.putString(key, value);
    }

    private Editor setInt(Editor editor, String key, int value) {
        return editor.putInt(key, value);
    }

    private Editor setLong(Editor editor, String key, long value) {
        return editor.putLong(key, value);
    }

    private Editor setBoolean(Editor editor, String key, boolean value) {
        return editor.putBoolean(key, value);
    }

    private void setStringArray(Editor editor, String key, String[] values) {
        setString(editor, key, JsonUtils.toJson(values));
    }
}
