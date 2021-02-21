package z.zer.tor.media.android.gui.adapters.menu;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.lang.ref.WeakReference;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.NetworkManager;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.gui.views.MenuAction;
import z.zer.tor.media.util.Ref;

final class OnBittorrentConnectRunnable implements Runnable {
    private final WeakReference<MenuAction> menuActionRef;

    OnBittorrentConnectRunnable(MenuAction menuAction) {
        menuActionRef = Ref.weak(menuAction);
    }

    public void run() {
        Engine.instance().startServices();
        while (!Engine.instance().isStarted()) {
            SystemClock.sleep(1000);
        }
        if (!Ref.alive(menuActionRef)) {
            return;
        }
        final MenuAction menuAction = menuActionRef.get();
        final Looper mainLooper = menuAction.getContext().getMainLooper();
        Handler h = new Handler(mainLooper);
        h.post(() -> menuAction.onClick(menuAction.getContext()));
    }

    void onBittorrentConnect(Context context) {
        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_BITTORRENT_ON_VPN_ONLY) &&
                !NetworkManager.instance().isTunnelUp()) {
            if (context instanceof Activity) {
                UIUtils.showShortMessage(((Activity) context).getWindow().getDecorView().getRootView(), R.string.cannot_start_engine_without_vpn);
            } else {
                UIUtils.showShortMessage(context, R.string.cannot_start_engine_without_vpn);
            }
        } else {
            Engine.instance().getThreadPool().execute(this);
        }
    }
}
