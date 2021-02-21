package z.zer.tor.media.android.gui;

import android.content.Context;
import android.view.ViewConfiguration;

import androidx.multidex.MultiDexApplication;

import com.andrew.apollo.cache.ImageCache;
import com.facebook.ads.AudienceNetworkAds;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Random;

import z.zer.tor.media.android.AndroidPlatform;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.util.ImageLoader;
import z.zer.tor.media.bittorrent.BTContext;
import z.zer.tor.media.bittorrent.BTEngine;
import z.zer.tor.media.platform.Platforms;
import z.zer.tor.media.platform.SystemPaths;
import z.zer.tor.media.search.CrawlPagedWebSearchPerformer;
import z.zer.tor.media.search.LibTorrentMagnetDownloader;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

import static z.zer.tor.media.android.util.Asyncs.async;
import static z.zer.tor.media.android.util.RunStrict.runStrict;

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
        AudienceNetworkAds.initialize(this);
    }

    @Override
    public void onLowMemory() {
        ImageCache.getInstance(this).evictAll();
        super.onLowMemory();
    }

    private void onCreateSafe() {
        ConfigurationManager.create(this);

        // some phones still can configure an external button as the
        // permanent menu key
//        ignoreHardwareMenu();

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
