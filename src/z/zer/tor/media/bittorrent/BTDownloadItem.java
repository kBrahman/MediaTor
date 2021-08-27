package z.zer.tor.media.bittorrent;

import com.frostwire.jlibtorrent.PiecesTracker;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentHandle;

import z.zer.tor.media.transfers.TransferItem;

import java.io.File;

public class BTDownloadItem implements TransferItem {

    private final TorrentHandle th;
    private final int index;

    private final File file;
    private final String name;
    private final long size;

    private PiecesTracker piecesTracker;

    public BTDownloadItem(TorrentHandle th, int index, String filePath, long fileSize, PiecesTracker piecesTracker) {
        this.th = th;
        this.index = index;

        this.file = new File(th.savePath(), filePath);
        this.name = file.getName();
        this.size = fileSize;

        this.piecesTracker = piecesTracker;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean isSkipped() {
        return th.filePriority(index) == Priority.IGNORE;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getDownloaded() {
        if (!th.isValid()) {
            return 0;
        }

        long[] progress = th.fileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY);
        return progress[index];
    }

    @Override
    public int getProgress() {
        if (!th.isValid() || size == 0) { // edge cases
            return 0;
        }

        int progress;
        long downloaded = getDownloaded();

        if (downloaded == size) {
            progress = 100;
        } else {
            progress = (int) ((float) (getDownloaded() * 100) / (float) size);
        }

        return progress;
    }

    @Override
    public boolean isComplete() {
        return getDownloaded() == size;
    }

    /**
     * @return
     */
    public long getSequentialDownloaded() {
        return piecesTracker != null ? piecesTracker.getSequentialDownloadedBytes(index) : 0;
    }
}
