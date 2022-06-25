package z.zer.tor.media.android.gui.transfers;

import static z.zer.tor.media.android.util.Asyncs.async;

import android.os.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.transfers.BittorrentDownload;
import z.zer.tor.media.transfers.Transfer;
import z.zer.tor.media.util.Logger;

public final class TransferManager {

    private static final Logger LOG = Logger.getLogger(TransferManager.class);

    private final List<Transfer> httpDownloads;
    private final List<BittorrentDownload> bittorrentDownloadsList;
    private final Map<String, BittorrentDownload> bittorrentDownloadsMap;
    private int downloadsToReview;
    private final Object alreadyDownloadingMonitor = new Object();
    private volatile static TransferManager instance;

    public static TransferManager instance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    private TransferManager() {
        this.httpDownloads = new CopyOnWriteArrayList<>();
        this.bittorrentDownloadsList = new CopyOnWriteArrayList<>();
        this.bittorrentDownloadsMap = new HashMap<>(0);
        this.downloadsToReview = 0;
        async(this::loadTorrentsTask);
    }

    public void reset() {
        clearTransfers();
        async(this::loadTorrentsTask);
    }

    public void onShutdown() {
        clearTransfers();
    }

    private void clearTransfers() {
        this.httpDownloads.clear();
        this.bittorrentDownloadsList.clear();
        this.bittorrentDownloadsMap.clear();
        this.downloadsToReview = 0;
    }

    /**
     * Is it using the SD Card's private (non-persistent after uninstall) app folder to save
     * downloaded files?
     */
    public static boolean isUsingSDCardPrivateStorage() {
        String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String currentPath = ConfigurationManager.instance().getStoragePath();

        return !primaryPath.equals(currentPath);
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<>();

        if (httpDownloads != null) {
            transfers.addAll(httpDownloads);
        }

        if (bittorrentDownloadsList != null) {
            transfers.addAll(bittorrentDownloadsList);
        }

        return transfers;
    }

    private boolean alreadyDownloading(String detailsUrl) {
        synchronized (alreadyDownloadingMonitor) {
            for (Transfer dt : httpDownloads) {
                if (dt.isDownloading()) {
                    if (dt.getName() != null && dt.getName().equals(detailsUrl)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getDownloadsToReview() {
        return downloadsToReview;
    }

    public void incrementDownloadsToReview() {
        downloadsToReview++;
    }

    public boolean remove(Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            bittorrentDownloadsMap.remove(((BittorrentDownload) transfer).getInfoHash());
            return bittorrentDownloadsList.remove(transfer);
        } else if (transfer instanceof Transfer) {
            return httpDownloads.remove(transfer);
        }
        return false;
    }

    public void pauseTorrents() {
        for (BittorrentDownload d : bittorrentDownloadsList) {
            if (!d.isSeeding()) {
                d.pause();
            }
        }
    }


    private void loadTorrentsTask() {
        bittorrentDownloadsList.clear();
        bittorrentDownloadsMap.clear();
    }
}
