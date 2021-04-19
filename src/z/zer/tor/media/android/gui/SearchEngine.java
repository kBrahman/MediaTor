/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package z.zer.tor.media.android.gui;

import android.os.Build;

import java.util.Arrays;
import java.util.List;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.search.SearchPerformer;
import z.zer.tor.media.search.archiveorg.ArchiveorgSearchPerformer;
import z.zer.tor.media.search.eztv.EztvSearchPerformer;
import z.zer.tor.media.search.frostclick.FrostClickSearchPerformer;
import z.zer.tor.media.search.frostclick.UserAgent;
import z.zer.tor.media.search.limetorrents.LimeTorrentsSearchPerformer;
import z.zer.tor.media.search.pixabay.PixabaySearchPerformer;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer;
import z.zer.tor.media.search.torlock.TorLockSearchPerformer;
import z.zer.tor.media.search.torrentdownloads.TorrentDownloadsSearchPerformer;
import z.zer.tor.media.search.tpb.TPBSearchPerformer;
import z.zer.tor.media.search.yify.YifySearchPerformer;
import z.zer.tor.media.search.zooqle.ZooqleSearchPerformer;

public abstract class SearchEngine {

    private static final UserAgent FROSTWIRE_ANDROID_USER_AGENT = new UserAgent(getOSVersionString(), Constants.FROSTWIRE_VERSION_STRING, Constants.MEDIAT_TOR_BUILD);
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

    @Override
    public String toString() {
        return name;
    }

    public static List<SearchEngine> getEngines() {
        // ensure that at leas one is enable
        boolean oneEnabled = false;
        for (SearchEngine se : ALL_ENGINES) {
            if (se.isEnabled()) {
                oneEnabled = true;
            }
        }
        if (!oneEnabled) {
            SearchEngine engineToEnable;
            engineToEnable = EZTV;

            // null check in case the logic above changes
            String prefKey = engineToEnable.getPreferenceKey();
            ConfigurationManager.instance().setBoolean(prefKey, true);
        }

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

    static String getOSVersionString() {
        return Build.VERSION.CODENAME + "_" + Build.VERSION.INCREMENTAL + "_" + Build.VERSION.RELEASE + "_" + Build.VERSION.SDK_INT;
    }

    public static final SearchEngine ZOOQLE = new SearchEngine("Zooqle", Constants.PREF_KEY_SEARCH_USE_ZOOQLE) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ZooqleSearchPerformer("zooqle.com", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine SOUNCLOUD = new SearchEngine("Soundcloud", Constants.PREF_KEY_SEARCH_USE_SOUNDCLOUD) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new SoundCloudSearchPerformer("api.sndcdn.com", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine ARCHIVE = new SearchEngine("Archive.org", Constants.PREF_KEY_SEARCH_USE_ARCHIVEORG) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ArchiveorgSearchPerformer("archive.org", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine FROSTCLICK = new SearchEngine("FrostClick", Constants.PREF_KEY_SEARCH_USE_FROSTCLICK) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new FrostClickSearchPerformer("api.frostclick.com", token, keywords, DEFAULT_TIMEOUT, FROSTWIRE_ANDROID_USER_AGENT);
        }
    };

    public static final SearchEngine TORLOCK = new SearchEngine("TorLock", Constants.PREF_KEY_SEARCH_USE_TORLOCK) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new TorLockSearchPerformer("www.torlock.com", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine TORRENTDOWNLOADS = new SearchEngine("TorrentDownloads", Constants.PREF_KEY_SEARCH_USE_TORRENTDOWNLOADS) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new TorrentDownloadsSearchPerformer("www.torrentdownloads.me", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine LIMETORRENTS = new SearchEngine("LimeTorrents", Constants.PREF_KEY_SEARCH_USE_LIMETORRENTS) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new LimeTorrentsSearchPerformer("www.limetorrents.cc", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine EZTV = new SearchEngine("Eztv", Constants.PREF_KEY_SEARCH_USE_EZTV) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new EztvSearchPerformer("eztv.ag", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine TPB = new SearchEngine("TPB", Constants.PREF_KEY_SEARCH_USE_TPB) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new TPBSearchPerformer("thepiratebay.org", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine YIFY = new SearchEngine("Yify", Constants.PREF_KEY_SEARCH_USE_YIFY) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new YifySearchPerformer("www.yify-torrent.org", token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine PIXABAY = new SearchEngine("Pixabay", Constants.PREF_KEY_SEARCH_USE_PIXABAY) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new PixabaySearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    private static final List<SearchEngine> ALL_ENGINES = Arrays.asList(YIFY, FROSTCLICK, ZOOQLE, TPB, SOUNCLOUD, ARCHIVE, PIXABAY, TORLOCK, TORRENTDOWNLOADS, LIMETORRENTS, EZTV);
}
