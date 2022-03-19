package z.zer.tor.media.android.gui;

import static z.zer.tor.media.android.util.Asyncs.async;
import static z.zer.tor.media.android.util.RunStrict.runStrict;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import com.andrew.apollo.cache.ImageCache;
import com.facebook.ads.AudienceNetworkAds;

import org.apache.commons.io.FileUtils;

import java.io.File;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.util.ImageLoader;
import z.zer.tor.media.platform.Platforms;
import z.zer.tor.media.search.CrawlPagedWebSearchPerformer;
import z.zer.tor.media.util.Logger;

public class MainApplication extends MultiDexApplication {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();
        runStrict(this::onCreateSafe);
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

        AbstractActivity.setMenuIconsVisible();
        NetworkManager.create(this);
        async(NetworkManager.instance(), NetworkManager::queryNetworkStatusBackground);
    }

    private void initializeCrawlPagedWebSearchPerformer(Context context) {
        CrawlPagedWebSearchPerformer.setCache(new DiskCrawlCache(context));
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
