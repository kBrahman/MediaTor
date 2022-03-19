/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
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

package z.zer.tor.media.android.gui.transfers;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.MediaStore;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.core.FileDescriptor;
import z.zer.tor.media.android.core.providers.TableFetcher;
import z.zer.tor.media.android.core.providers.TableFetchers;
import z.zer.tor.media.android.gui.Librarian;
import z.zer.tor.media.android.gui.NetworkManager;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.platform.Platforms;
import z.zer.tor.media.transfers.BittorrentDownload;
import z.zer.tor.media.transfers.TransferItem;
import z.zer.tor.media.transfers.TransferState;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author gubatron
 * @author aldenml
 */
public final class UIBittorrentDownload {

    private static final Logger LOG = Logger.getLogger(UIBittorrentDownload.class);


    public UIBittorrentDownload() {
    }

    private static boolean deleteEmptyDirectoryRecursive(File directory) {
        // make sure we only delete canonical children of the parent file we
        // wish to delete. I have a hunch this might be an issue on OSX and
        // Linux under certain circumstances.
        // If anyone can test whether this really happens (possibly related to
        // symlinks), I would much appreciate it.
        String canonicalParent;
        try {
            canonicalParent = directory.getCanonicalPath();
        } catch (IOException ioe) {
            return false;
        }

        if (!directory.isDirectory()) {
            return false;
        }

        boolean canDelete = true;

        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                try {
                    if (!file.getCanonicalPath().startsWith(canonicalParent))
                        continue;
                } catch (IOException ioe) {
                    canDelete = false;
                }
                if (!deleteEmptyDirectoryRecursive(file)) {
                    canDelete = false;
                }
            }
        }

        return canDelete && directory.delete();
    }

}
