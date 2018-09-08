/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 * Marcelina Knitter (@marcelinkaaa)
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

package zig.zak.media.tor.android.gui.adapters.menu;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import zig.zak.media.tor.R;
import zig.zak.media.tor.android.core.ConfigurationManager;
import zig.zak.media.tor.android.core.Constants;
import zig.zak.media.tor.android.gui.NetworkManager;
import zig.zak.media.tor.android.gui.services.Engine;
import zig.zak.media.tor.android.gui.util.UIUtils;
import zig.zak.media.tor.android.gui.views.MenuAction;
import zig.zak.media.tor.util.Ref;

import java.lang.ref.WeakReference;

/**
 * @author aldenml
 * @author gubatron
 * @author marcelinkaaa
 *         Created on 8/28/17.
 */


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
