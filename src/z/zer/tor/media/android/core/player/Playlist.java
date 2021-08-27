package z.zer.tor.media.android.core.player;

import java.util.List;

public interface Playlist {
    
    List<PlaylistItem> getItems();
    
    PlaylistItem getCurrentItem();
}
