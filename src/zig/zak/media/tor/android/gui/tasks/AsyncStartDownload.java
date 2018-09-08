/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
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

package zig.zak.media.tor.android.gui.tasks;

import android.app.Activity;
import android.content.Context;

import zig.zak.media.tor.R;
import zig.zak.media.tor.android.gui.dialogs.HandpickedTorrentDownloadDialogOnFetch;
import zig.zak.media.tor.android.gui.dialogs.YouTubeDownloadDialog;
import zig.zak.media.tor.android.gui.transfers.ExistingDownload;
import zig.zak.media.tor.android.gui.transfers.InvalidDownload;
import zig.zak.media.tor.android.gui.transfers.InvalidTransfer;
import zig.zak.media.tor.android.gui.transfers.TransferManager;
import zig.zak.media.tor.android.gui.util.UIUtils;
import zig.zak.media.tor.search.SearchResult;
import zig.zak.media.tor.search.torrent.TorrentCrawledSearchResult;
import zig.zak.media.tor.search.torrent.TorrentSearchResult;
import zig.zak.media.tor.search.youtube.YouTubePackageSearchResult;
import zig.zak.media.tor.transfers.BittorrentDownload;
import zig.zak.media.tor.transfers.Transfer;
import zig.zak.media.tor.util.Logger;

import static zig.zak.media.tor.android.util.Asyncs.async;

/**
 * @author gubatron
 * @author aldenml
 */
public class AsyncStartDownload {

    private static final Logger LOG = Logger.getLogger(AsyncStartDownload.class);

    public AsyncStartDownload(final Context ctx, final SearchResult sr, final String message) {
        async(ctx, AsyncStartDownload::doInBackground, sr, message, AsyncStartDownload::onPostExecute);
    }

    public AsyncStartDownload(final Context ctx, final SearchResult sr) {
        this(ctx, sr, null);
    }

    private static Transfer doInBackground(final Context ctx, final SearchResult sr, final String message) {
        Transfer transfer = null;
        try {
            if (sr instanceof TorrentSearchResult && !(sr instanceof TorrentCrawledSearchResult)) {
                transfer = TransferManager.instance().downloadTorrent(((TorrentSearchResult) sr).getTorrentUrl(), new HandpickedTorrentDownloadDialogOnFetch((Activity) ctx), sr.getDisplayName());
            } else if (sr instanceof YouTubePackageSearchResult) {
                //FIXME: this stuff shouldn't be in the background!!!
                YouTubeDownloadDialog ytDownloadDlg = YouTubeDownloadDialog.newInstance(ctx, (YouTubePackageSearchResult) sr);
                ytDownloadDlg.show(((Activity) ctx).getFragmentManager());
            } else {
                transfer = TransferManager.instance().download(sr);
                if (!(transfer instanceof InvalidDownload)) {
                    //FIXME: this stuff shouldn't be in the background thread
                    UIUtils.showTransfersOnDownloadStart(ctx);
                }
            }
        } catch (Throwable e) {
            LOG.warn("Error adding new download from result: " + sr, e);
            e.printStackTrace();
        }
        return transfer;
    }

    private static void onPostExecute(final Context ctx, final SearchResult sr, final String message, final Transfer transfer) {
        if (transfer != null) {
            if (!(transfer instanceof InvalidTransfer)) {
                TransferManager tm = TransferManager.instance();
                if (tm.isBittorrentDownloadAndMobileDataSavingsOn(transfer)) {
                    UIUtils.showLongMessage(ctx, R.string.torrent_transfer_enqueued_on_mobile_data);
                    ((BittorrentDownload) transfer).pause();
                } else {
                    if (tm.isBittorrentDownloadAndMobileDataSavingsOff(transfer)) {
                        UIUtils.showLongMessage(ctx, R.string.torrent_transfer_consuming_mobile_data);
                    }

                    if (message != null) {
                        UIUtils.showShortMessage(ctx, message);
                    }
                }
                UIUtils.showTransfersOnDownloadStart(ctx);
            } else if (!(transfer instanceof ExistingDownload)) {
                UIUtils.showLongMessage(ctx, ((InvalidTransfer) transfer).getReasonResId());
            }
        }
    }
}
