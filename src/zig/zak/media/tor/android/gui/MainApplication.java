/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
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

package zig.zak.media.tor.android.gui;

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.view.ViewConfiguration;

import com.andrew.apollo.cache.ImageCache;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Random;

import zig.zak.media.tor.android.AndroidPlatform;
import zig.zak.media.tor.android.core.ConfigurationManager;
import zig.zak.media.tor.android.core.Constants;
import zig.zak.media.tor.android.gui.views.AbstractActivity;
import zig.zak.media.tor.android.util.ImageLoader;
import zig.zak.media.tor.bittorrent.BTContext;
import zig.zak.media.tor.bittorrent.BTEngine;
import zig.zak.media.tor.platform.Platforms;
import zig.zak.media.tor.platform.SystemPaths;
import zig.zak.media.tor.search.CrawlPagedWebSearchPerformer;
import zig.zak.media.tor.search.LibTorrentMagnetDownloader;
import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.Ref;

import static zig.zak.media.tor.android.util.Asyncs.async;
import static zig.zak.media.tor.android.util.RunStrict.runStrict;

public class MainApplication extends MultiDexApplication {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();
        runStrict(this::onCreateSafe);
        Platforms.set(new AndroidPlatform(this));
        new Thread(new BTEngineInitializer(Ref.weak(this))).start();
        ImageLoader.start(this);
        async(this, this::initializeCrawlPagedWebSearchPerformer);
        async(LocalSearchEngine::instance);
        async(MainApplication::cleanTemp);
    }

    @Override
    public void onLowMemory() {
        ImageCache.getInstance(this).evictAll();
        ImageLoader.getInstance(this).clear();
        super.onLowMemory();
    }

    private void onCreateSafe() {
        ConfigurationManager.create(this);

        // some phones still can configure an external button as the
        // permanent menu key
        ignoreHardwareMenu();

        AbstractActivity.setMenuIconsVisible(true);
        NetworkManager.create(this);
        async(NetworkManager.instance(), NetworkManager::queryNetworkStatusBackground);
    }

    private void ignoreHardwareMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            @SuppressWarnings("JavaReflectionMemberAccess") Field f = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (f != null) {
                f.setAccessible(true);
                f.setBoolean(config, false);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    private void initializeCrawlPagedWebSearchPerformer(Context context) {
        CrawlPagedWebSearchPerformer.setCache(new DiskCrawlCache(context));
        CrawlPagedWebSearchPerformer.setMagnetDownloader(new LibTorrentMagnetDownloader());
    }

    // don't try to refactor this into an async call since this guy runs on a thread
    // outside the Engine threadpool
    private static class BTEngineInitializer implements Runnable {
        private final WeakReference<Context> mainAppRef;

        BTEngineInitializer(WeakReference<Context> mainAppRef) {
            this.mainAppRef = mainAppRef;
        }

        public void run() {
            SystemPaths paths = Platforms.get().systemPaths();

            BTContext ctx = new BTContext();
            ctx.homeDir = paths.libtorrent();
            ctx.torrentsDir = paths.torrents();
            ctx.dataDir = paths.data();
            ctx.optimizeMemory = true;

            // port range [37000, 57000]
            int port0 = 37000 + new Random().nextInt(20000);
            int port1 = port0 + 10; // 10 retries
            String iface = "0.0.0.0:%1$d,[::]:%1$d";
            ctx.interfaces = String.format(Locale.US, iface, port0);
            ctx.retries = port1 - port0;

            ctx.enableDht = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_ENABLE_DHT);


            BTEngine.ctx = ctx;
            BTEngine.onCtxSetupComplete();
            BTEngine.getInstance().start();

            syncMediaStore();
        }

        private void syncMediaStore() {
            if (Ref.alive(mainAppRef)) {
                Librarian.instance().syncMediaStore(mainAppRef);
            } else {
                LOG.warn("syncMediaStore() failed, lost MainApplication reference");
            }
        }
    }

    private static void cleanTemp() {
        try {
            File tmp = Platforms.get().systemPaths().temp();
            if (tmp.exists()) {
                FileUtils.cleanDirectory(tmp);
            }
        } catch (Throwable e) {
            LOG.error("Error during setup of temp directory", e);
        }
    }
}
