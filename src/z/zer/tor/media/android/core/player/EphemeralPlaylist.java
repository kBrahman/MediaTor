package z.zer.tor.media.android.core.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import z.zer.tor.media.android.core.FileDescriptor;

public final class EphemeralPlaylist implements Playlist {

    private final List<PlaylistItem> items;

    private int currentIndex;

    public EphemeralPlaylist(List<FileDescriptor> fds) {
        this.items = new ArrayList<>();

        for (FileDescriptor fd : fds) {
            this.items.add(new PlaylistItem(fd));
        }

        Collections.sort(this.items, (a, b) -> {
            if (a.getFD().dateAdded == b.getFD().dateAdded) {
                return 0;
            }
            return (a.getFD().dateAdded > b.getFD().dateAdded) ? -1 : 1;
        });

        this.currentIndex = -1;
    }

    public void setNextItem(PlaylistItem playlistItem) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).equals(playlistItem)) {
                currentIndex = index;
                break;
            }
        }
    }

    @Override
    public List<PlaylistItem> getItems() {
        return items;
    }

    @Override
    public PlaylistItem getCurrentItem() {
        if (currentIndex >= 0) {
            return items.get(currentIndex);
        }
        return null;
    }
}
