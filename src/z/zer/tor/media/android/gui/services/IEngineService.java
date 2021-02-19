package z.zer.tor.media.android.gui.services;

import android.app.Application;

import java.io.File;

import z.zer.tor.media.android.core.player.CoreMediaPlayer;

public interface IEngineService {

    byte STATE_INVALID = -1;
    byte STATE_STARTED = 10;
    byte STATE_STARTING = 11;
    byte STATE_STOPPED = 12;
    byte STATE_STOPPING = 13;
    byte STATE_DISCONNECTED = 14;

    CoreMediaPlayer getMediaPlayer();

    byte getState();

    boolean isStarted();

    boolean isStarting();

    boolean isStopped();

    boolean isStopping();

    boolean isDisconnected();

    void startServices();

    void stopServices(boolean disconnected);

    /**
     * @param displayName the display name to show in the notification
     * @param file        the file to open
     * @param infoHash    the optional info hash if available
     */
    void notifyDownloadFinished(String displayName, File file, String infoHash);

    Application getApplication();

    void shutdown();
}
