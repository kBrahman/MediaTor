package z.zer.tor.media.android.core.player;

import android.content.Context;

import z.zer.tor.media.android.core.FileDescriptor;

public interface CoreMediaPlayer {

    void play(Playlist playlist);

    void stop();

    boolean isPlaying();

    /**
     * The current file the media player is playing.
     *
     * @return
     */
    FileDescriptor getCurrentFD(final Context context);

    /**
     * The current file the simple media player is playing.
     *
     * @return
     */
    FileDescriptor getSimplePlayerCurrentFD(final Context context);
}
