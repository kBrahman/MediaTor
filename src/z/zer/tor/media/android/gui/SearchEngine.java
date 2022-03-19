package z.zer.tor.media.android.gui;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.search.SearchPerformer;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer;

public abstract class SearchEngine {

    private static final int DEFAULT_TIMEOUT = 10000;

    private final String name;
    private final String preferenceKey;

    private boolean active;

    private SearchEngine(String name, String preferenceKey) {
        this.name = name;
        this.preferenceKey = preferenceKey;
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public abstract SearchPerformer getPerformer(long token, String keywords);

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public boolean isEnabled() {
        return isActive() && ConfigurationManager.instance().getBoolean(preferenceKey);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    public static List<SearchEngine> getEngines() {
        return ALL_ENGINES;
    }

    public static SearchEngine forName(String name) {
        for (SearchEngine engine : getEngines()) {
            if (engine.getName().equalsIgnoreCase(name)) {
                return engine;
            }
        }

        return null;
    }

    public static final SearchEngine SOUNCLOUD = new SearchEngine("Soundcloud", Constants.PREF_KEY_SEARCH_USE_SOUNDCLOUD) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new SoundCloudSearchPerformer("api.sndcdn.com", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    private static final List<SearchEngine> ALL_ENGINES = Collections.singletonList(SOUNCLOUD);
}
