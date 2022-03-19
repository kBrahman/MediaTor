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

package z.zer.tor.media.android.gui.transfers;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import z.zer.tor.media.transfers.BittorrentDownload;
import z.zer.tor.media.transfers.TransferItem;
import z.zer.tor.media.transfers.TransferState;
import z.zer.tor.media.util.HttpClientFactory;
import z.zer.tor.media.util.Logger;

/**
 * @author gubatron
 * @author aldenml
 */
public class TorrentFetcherDownload implements BittorrentDownload {

    private static final Logger LOG = Logger.getLogger(TorrentFetcherDownload.class);

    private final TransferManager manager;
    private final TorrentDownloadInfo info;
    private final Date created;
    private final TorrentFetcherListener fetcherListener;
    public final long tokenId;

    private TransferState state;

    TorrentFetcherDownload(TransferManager manager, TorrentDownloadInfo info) {
        this(manager, info, null);
    }

    public TorrentFetcherDownload(TransferManager manager, TorrentDownloadInfo info, TorrentFetcherListener listener) {
        this.manager = manager;
        this.info = info;
        this.created = new Date();
        this.fetcherListener = listener;
        this.tokenId = new Random(System.currentTimeMillis()).nextLong();
        state = TransferState.DOWNLOADING_TORRENT;

        Thread t = new Thread(new FetcherRunnable(), "Torrent-Fetcher - " + info.getTorrentUrl());
        t.setDaemon(true);
        t.start();
    }

    String getTorrentUri() {
        return info.getTorrentUrl();
    }

    public String getDisplayName() {
        return info.getDisplayName();
    }

    @Override
    public TransferState getState() {
        return state;
    }

    public int getProgress() {
        return 0;
    }

    public long getSize() {
        return info.getSize();
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public List<TransferItem> getItems() {
        return Collections.emptyList();
    }

    public File getSavePath() {
        return null;
    }

    @Override
    public File getContentSavePath() {
        return null;
    }

    public long getBytesReceived() {
        return 0;
    }

    public long getBytesSent() {
        return 0;
    }

    public long getDownloadSpeed() {
        return 0;
    }

    public long getUploadSpeed() {
        return 0;
    }

    public long getETA() {
        return 0;
    }

    public String getInfoHash() {
        return info.getHash();
    }

    @Override
    public int getConnectedPeers() {
        return 0;
    }

    @Override
    public int getTotalPeers() {
        return 0;
    }

    @Override
    public int getConnectedSeeds() {
        return 0;
    }

    @Override
    public int getTotalSeeds() {
        return 0;
    }

    @Override
    public String magnetUri() {
        return info.makeMagnetUri();
    }


    public String getPeers() {
        return "";
    }

    public String getSeeds() {
        return "";
    }

    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isDownloading() {
        return true;
    }

    @Override
    public boolean isSeeding() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void remove(boolean deleteData) {
        state = TransferState.CANCELED;
        manager.remove(this);
    }

    @Override
    public void remove(boolean deleteTorrent, boolean deleteData) {
        remove(deleteData);
    }

    @Override
    public String getPredominantFileExtension() {
        return "torrent";
    }

    public void pause() {
    }

    public void resume() {
    }

    @Override
    public String getName() {
        return info.getDetailsUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TorrentFetcherDownload)) {
            return false;
        }

        String u1 = info.getTorrentUrl();
        String u2 = ((TorrentFetcherDownload) o).info.getTorrentUrl();

        return u1.equalsIgnoreCase(u2);
    }

    @Override
    public File previewFile() {
        return null;
    }

    private class FetcherRunnable implements Runnable {

        @Override
        public void run() {
            if (state == TransferState.CANCELED) {
                return;
            }

            try {
                byte[] data;
                String uri = info.getTorrentUrl();
                String referrer = info.getReferrerUrl();
                if (uri.startsWith("http")) {
                    // use our http client, since we can handle referer
                    data = HttpClientFactory.getInstance(HttpClientFactory.HttpContext.DOWNLOAD).getBytes(uri, 30000, referrer);
                }

                if (state == TransferState.CANCELED) {
                    return;
                }

                state = TransferState.ERROR;
            } catch (Throwable e) {
                state = TransferState.ERROR;
                LOG.error("Error downloading torrent from uri", e);
            }
        }
    }
}
