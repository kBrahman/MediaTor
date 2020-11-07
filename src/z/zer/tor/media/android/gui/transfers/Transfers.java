package z.zer.tor.media.android.gui.transfers;

import z.zer.tor.media.bittorrent.BTDownload;
import z.zer.tor.media.transfers.Transfer;
import z.zer.tor.media.transfers.TransferItem;
import z.zer.tor.media.util.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gubatron
 * @author aldenml
 */
public final class Transfers {

    private static final Logger LOG = Logger.getLogger(Transfers.class);

    private Transfers() {
    }

    public static Set<File> getSkipedFiles() {
        Set<File> set = new HashSet<>();
        List<Transfer> transfers = TransferManager.instance().getTransfers();

        for (Transfer t : transfers) {
            if (t instanceof UIBittorrentDownload) {
                set.addAll(getSkippedFiles(((UIBittorrentDownload) t).getDl()));
            }
        }

        return set;
    }

    public static Set<File> getSkippedFiles(BTDownload dl) {
        Set<File> set = new HashSet<>();
        List<TransferItem> items = dl.getItems();
        for (TransferItem item : items) {
            try {
                if (item.isSkipped()) {
                    set.add(item.getFile());
                }
            } catch (Throwable e) {
                LOG.error("Error getting file information", e);
            }
        }
        return set;
    }

    public static Set<File> getIncompleteFiles() {
        Set<File> set = new HashSet<>();
        List<Transfer> transfers = TransferManager.instance().getTransfers();

        for (Transfer t : transfers) {
            if (t instanceof UIBittorrentDownload) {
                set.addAll(((UIBittorrentDownload) t).getDl().getIncompleteFiles());
            }
        }

        return set;
    }

    public static Set<File> getIgnorableFiles() {
        Set<File> set = getIncompleteFiles();
        set.addAll(getSkipedFiles());
        return set;
    }
}
