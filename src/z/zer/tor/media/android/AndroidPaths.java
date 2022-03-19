/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package z.zer.tor.media.android;

import android.app.Application;
import android.os.Environment;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.platform.SystemPaths;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

final class AndroidPaths implements SystemPaths {

    private static final String STORAGE_PATH = "MediaTor";
    private static final String TORRENT_DATA_PATH = "TorrentsData";
    private static final String TORRENTS_PATH = "Torrents";
    private static final String TEMP_PATH = "temp";
    private static final String LIBTORRENT_PATH = "libtorrent";
    private final Application app;

    public AndroidPaths(Application app) {
        this.app = app;
    }

    @Override
    public File data() {
        return new File(storage(), TORRENT_DATA_PATH);
    }

    @Override
    public File torrents() {
        return new File(storage(), TORRENTS_PATH);
    }

    @Override
    public File temp() {
        return new File(app.getExternalFilesDir(null), TEMP_PATH);
    }

    @Override
    public File libtorrent() {
        return new File(app.getExternalFilesDir(null), LIBTORRENT_PATH);
    }

    private static File storage() {
        CopyOnWriteArrayList<Integer> list=new CopyOnWriteArrayList<>();
        list.add(1);
        String path = ConfigurationManager.instance().getString(Constants.PREF_KEY_STORAGE_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        if (path.toLowerCase().endsWith("/" + STORAGE_PATH.toLowerCase())) {
            return new File(path);
        } else {
            return new File(path, STORAGE_PATH);
        }
    }
}
