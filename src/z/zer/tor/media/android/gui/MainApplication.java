package z.zer.tor.media.android.gui;

import static z.zer.tor.media.android.util.Asyncs.async;
import static z.zer.tor.media.android.util.RunStrict.runStrict;

import androidx.multidex.MultiDexApplication;

import com.facebook.ads.AudienceNetworkAds;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.util.ImageLoader;
import z.zer.tor.media.util.Logger;

public class MainApplication extends MultiDexApplication {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();
        runStrict(this::onCreateSafe);
        ImageLoader.start(this);
        async(LocalSearchEngine::instance);
        AudienceNetworkAds.initialize(this);
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

}
