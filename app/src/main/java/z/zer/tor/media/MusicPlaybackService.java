package z.zer.tor.media;

import static z.zer.tor.media.android.util.Asyncs.async;
import static z.zer.tor.media.android.util.RunStrict.runStrict;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.StaleDataException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.Log;

import androidx.core.content.ContextCompat;

import z.zer.tor.media.utils.MusicUtils;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Stack;

import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.util.Asyncs;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

/**
 * A background {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
public class MusicPlaybackService extends Service {
    private static final Logger LOG = Logger.getLogger(MusicPlaybackService.class);
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Indicates that the music has paused or resumed
     */
    public static final String PLAYSTATE_CHANGED = "zig.zak.media.tor.playstatechanged";

    /**
     * Indicates has been stopped
     */
    private static final String PLAYSTATE_STOPPED = "zig.zak.media.tor.playstatestopped";

    /**
     * Indicates that music playback position within
     * a title was changed
     */
    private static final String POSITION_CHANGED = "zig.zak.media.tor.positionchanged";

    /**
     * Indicates the meta data has changed in some way, like a track change
     */
    public static final String META_CHANGED = "zig.zak.media.tor.metachanged";

    /**
     * Indicates the queue has been updated
     */
    private static final String QUEUE_CHANGED = "zig.zak.media.tor.queuechanged";

    /**
     * Indicates the repeat mode changed
     */
    public static final String REPEATMODE_CHANGED = "zig.zak.media.tor.repeatmodechanged";

    /**
     * Indicates the shuffle mode changed
     */
    public static final String SHUFFLEMODE_CHANGED = "zig.zak.media.tor.shufflemodechanged";

    /**
     * For backwards compatibility reasons, also provide sticky
     * broadcasts under the music package
     */
    private static final String APOLLO_PACKAGE_NAME = "zig.zak.media.tor";
    private static final String MUSIC_PACKAGE_NAME = "com.android.music";
    static final String SERVICECMD = "zig.zak.media.tor.musicservicecommand";

    /**
     * Called to go toggle between pausing and playing the music
     */
    static final String TOGGLEPAUSE_ACTION = "zig.zak.media.tor.togglepause";

    /**
     * Called to go to pause the playback
     */
    private static final String PAUSE_ACTION = "zig.zak.media.tor.pause";

    /**
     * Called to go to stop the playback
     */
    static final String STOP_ACTION = "zig.zak.media.tor.stop";

    /**
     * Called to go to the previous track
     */
    public static final String PREVIOUS_ACTION = "zig.zak.media.tor.previous";

    /**
     * Called to go to the next track
     */
    static final String NEXT_ACTION = "zig.zak.media.tor.next";

    /**
     * Called to change the repeat mode
     */
    private static final String REPEAT_ACTION = "zig.zak.media.tor.repeat";

    /**
     * Called to change the shuffle mode
     */
    private static final String SHUFFLE_ACTION = "zig.zak.media.tor.shuffle";

    /**
     * Called to update the service about the foreground state of Apollo's activities
     */
    public static final String FOREGROUND_STATE_CHANGED = "zig.zak.media.tor.fgstatechanged";

    public static final String NOW_IN_FOREGROUND = "nowinforeground";

    /**
     * Used to easily notify a list that it should refresh. i.e. A playlist
     * changes
     */
    public static final String REFRESH = "zig.zak.media.tor.refresh";

    /**
     * Used by the alarm intent to shutdown the service after being idle
     */
    public static final String SHUTDOWN_ACTION = "zig.zak.media.tor.shutdown";

    /**
     * Simple player stopped playing the sound (completed or was stopped)
     */
    public static final String SIMPLE_PLAYSTATE_STOPPED = "zig.zak.media.tor.simple.stopped";

    static final String CMDNAME = "command";

    static final String CMDTOGGLE_PAUSE = "togglepause";

    static final String CMDSTOP = "stop";

    static final String CMDPAUSE = "pause";

    static final String CMDPLAY = "play";

    static final String CMDPREVIOUS = "previous";

    static final String CMDNEXT = "next";

    private static final int IDCOLIDX = 0;

    /**
     * Moves a list to the front of the queue
     */
    private static final int NOW = 1;

    /**
     * Moves a list to the next position in the queue
     */
    public static final int NEXT = 2;

    /**
     * Moves a list to the last position in the queue
     */
    public static final int LAST = 3;

    /**
     * Turns repeat off
     */
    public static final int REPEAT_NONE = 0;

    /**
     * Repeats the current track in a list
     */
    public static final int REPEAT_CURRENT = 1;

    /**
     * Repeats all the tracks in a list
     */
    public static final int REPEAT_ALL = 2;

    /**
     * Indicates when the track ends
     */
    private static final int TRACK_ENDED = 1;

    /**
     * Indicates that the current track was changed the next track
     */
    private static final int TRACK_WENT_TO_NEXT = 2;

    /**
     * Indicates when the release the wake lock
     */
    private static final int RELEASE_WAKELOCK = 3;

    /**
     * Indicates the player died
     */
    private static final int SERVER_DIED = 4;

    /**
     * Indicates some sort of focus change, maybe a phone call
     */
    private static final int FOCUS_CHANGE = 5;

    /**
     * Indicates to fade the volume down
     */
    private static final int FADE_DOWN = 6;

    /**
     * Indicates to fade the volume back up
     */
    private static final int FADE_UP = 7;

    /**
     * Idle time before stopping the foreground notification (1 minute)
     */
    private static final int IDLE_DELAY = 60000;

    /**
     * Song play time used as threshold for rewinding to the beginning of the
     * track instead of skipping to the previous track when getting the PREVIOUS
     * command
     */
    private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;

    /**
     * The max size allowed for the track history
     */
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * The columns used to retrieve any info from the current track
     */
    private static final String[] PROJECTION = new String[]{
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };

    /**
     * The columns used to retrieve any info from the current album
     */
    private static final String[] ALBUM_PROJECTION = new String[]{
            MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR
    };

    /**
     * The columns used to retrieve any info from the current track
     */
    private static final String[] SIMPLE_PROJECTION = new String[]{
            "_id", MediaStore.Audio.Media.DATA
    };


    /**
     * Keeps a mapping of the track history
     */
    private static final Stack<Integer> mHistory = new Stack<>();

    /**
     * Used to save the queue as reverse hexadecimal numbers, which we can
     * generate faster than normal decimal or hexadecimal numbers, which in
     * turn allows us to save the playlist more often without worrying too
     * much about performance
     */
    private static final char HEX_DIGITS[] = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };


    /**
     * The media player
     */
    private MultiPlayer mPlayer;

    /**
     * Simple Media Player used for out of order sounds playback
     * Trumped by mPlayer play actions
     */
    private MediaPlayer mSimplePlayer;

    /**
     * Path to file currently played by SimplePlayer
     */
    private String mSimplePlayerPlayingFile;


    /**
     * The path of the current file to play
     */
    private String mFileToPlay;

    /**
     * Keeps the service running when the screen is off
     */
    private WakeLock mWakeLock;

    /**
     * Alarm intent for removing the notification when nothing is playing
     * for some time
     */
    private AlarmManager mAlarmManager;
    private PendingIntent mShutdownIntent;
    private boolean mShutdownScheduled;

    /**
     * The cursor used to retrieve info on the current track and run the
     * necessary queries to play audio files
     */
    private Cursor mCursor;

    private final Object cursorLock = new Object();
    private final Object audioSessionIdLock = new Object();

    /**
     * The cursor used to retrieve info on the album the current track is
     * part of, if any.
     */
    private Cursor mAlbumCursor;

    /**
     * Monitors the audio state
     */
    private AudioManager mAudioManager;

    /**
     * Settings used to save and retrieve the queue and history
     */
    private SharedPreferences mPreferences;

    /**
     * Used to know when the service is active
     */
    private boolean mServiceInUse = false;

    /**
     * Used to know if something should be playing or not
     */
    private boolean mIsSupposedToBePlaying = false;

    /**
     * Used to indicate if the queue can be saved
     */
    private boolean mQueueIsSaveable = true;

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private boolean mPausedByTransientLossOfFocus = false;

    /**
     * Used to track whether any of Apollo's activities is in the foreground
     */
    private boolean musicPlaybackActivityInForeground = false;

    /**
     * Lock screen controls
     */
    private RemoteControlClient mRemoteControlClient;


    // We use this to distinguish between different cards when saving/restoring
    // playlists
    private int mCardId;

    private int mPlayListLen = 0;

    private int mPlayPos = -1;

    private int mNextPlayPos = -1;

    private int mOpenFailedCounter = 0;

    private int mMediaMountedCount = 0;

    private boolean mShuffleEnabled = false;

    private int mRepeatMode = REPEAT_ALL;

    private int mServiceStartId = -1;

    private long[] mPlayList = null;

    private BroadcastReceiver mUnmountReceiver = null;


    private boolean launchPlayerActivity;

    private boolean exiting;

    private final Random r = new Random();

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(final Intent intent) {
        if (D) LOG.info("Service bound, intent = " + intent);
        cancelShutdown();
        mServiceInUse = true;
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onUnbind(final Intent intent) {
        if (D) LOG.info("onUnbind()");
        if (exiting) {
            LOG.info("onUnbind() aborted, already taken care of by releaseServiceUiAndStop() (exiting=true)");
            return true;
        }

        mServiceInUse = false;
        saveQueue(true);

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        }
        stopSelf(mServiceStartId);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRebind(final Intent intent) {
        cancelShutdown();
        mServiceInUse = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        if (D) LOG.info("Creating service");
        super.onCreate();

        boolean permissionGranted = runStrict(() ->
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    /**
     * Initializes the remote control client
     */
    private void setUpRemoteControlClient() {
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mRemoteControlClient = new RemoteControlClient(
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent,
                        PendingIntent.FLAG_IMMUTABLE));

        try {
            if (mAudioManager != null) {
                mAudioManager.registerRemoteControlClient(mRemoteControlClient);
            }
        } catch (Throwable t) {
            // seems like this doesn't work on some devices where it requires MODIFY_PHONE_STATE
            // which is a permission only given to system apps, not third party apps.
        }

        // Flags for the media transport control that this client supports.
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        flags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;
        mRemoteControlClient.setOnGetPlaybackPositionListener(this::position);
        mRemoteControlClient.setPlaybackPositionUpdateListener(this::seek);

        mRemoteControlClient.setTransportControlFlags(flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (D) LOG.info("Destroying service");
        super.onDestroy();

        // Tell any sound effect processors (e.g. equalizers) that we're leaving
        try {
            final Intent audioEffectsIntent = new Intent(
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(audioEffectsIntent);
        } catch (Throwable ignored) {
        }

        // remove any pending alarms
        // note: mShutdownIntent could be null because during creation
        // internally PendingIntent.getService is eating (and ignoring)
        // a possible RemoteException
        if (mAlarmManager != null && mShutdownIntent != null) {
            mAlarmManager.cancel(mShutdownIntent);
        }

        // Release the player
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        // Release simple player
        if (mSimplePlayer != null) {
            mSimplePlayer.release();
            mSimplePlayer = null;
        }

        // Remove the audio focus listener and lock screen controls
        if (mAudioManager != null) {
            mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
        }

        // Close the cursor
        closeCursor();

        // Unregister the mount listener
        try {
            unregisterReceiver(mIntentReceiver);
        } catch (Throwable ignored) {
        }

        if (mUnmountReceiver != null) {
            try {
                unregisterReceiver(mUnmountReceiver);
            } catch (Throwable ignored) {
            }
            mUnmountReceiver = null;
        }

        // Release the wake lock
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
            } catch (RuntimeException ignored) {
                // might be underlocked and otherwise causing a crash on shutdown
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (D) LOG.info("Got new intent " + intent + ", startId = " + startId);
        mServiceStartId = startId;

        if (intent != null) {
            final String action = intent.getAction();

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                musicPlaybackActivityInForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false);
            }

            if (SHUTDOWN_ACTION.equals(action)) {
                cancelShutdown();
                exiting = intent.hasExtra("force");
                releaseServiceUiAndStop(exiting);
                return START_NOT_STICKY;
            }

            handleCommandIntent(intent);
        }

        // Make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        scheduleDelayedShutdown();
        return START_STICKY;
    }

    /**
     * @return true if serviced was released.
     */
    private void releaseServiceUiAndStop(boolean force) {

        if (force && isPlaying()) {
            LOG.info("releaseServiceUiAndStop(force=true) : isPlaying()=" + isPlaying());
            stopPlayer();
        }

        updateRemoteControlClient(PLAYSTATE_STOPPED);

        if (!mServiceInUse || force) {
            saveQueue(true);
            stopSelf(mServiceStartId);
        }

        if (force) {
            // clear queue position
            mPreferences.edit().putInt("curpos", -1).apply();
        }

    }

    private void handleCommandIntent(Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;

        if (D) LOG.info("handleCommandIntent: action = " + action + ", command = " + command);

        if (CMDNEXT.equals(command) || NEXT_ACTION.equals(action)) {
            gotoNext(true);
        } else if (CMDPREVIOUS.equals(command) || PREVIOUS_ACTION.equals(action)) {
            if (position() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                prev();
            } else {
                seek(0);
                play();
            }
        } else if (CMDTOGGLE_PAUSE.equals(command) || TOGGLEPAUSE_ACTION.equals(action)) {
            if (isPlaying()) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else {
                play();
            }
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            pause();
            mPausedByTransientLossOfFocus = false;
        } else if (CMDPLAY.equals(command)) {
            play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            pause();
            mPausedByTransientLossOfFocus = false;
            seek(0);
            releaseServiceUiAndStop(false);
        } else if (REPEAT_ACTION.equals(action)) {
            cycleRepeat();
        }
    }


    /**
     * @return A card ID used to save and restore playlists, i.e., the queue.
     */
    private int getCardId() {
        int mCardId = -1;
        try {
            final ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(Uri.parse("content://media/external/fs_id"), null, null,
                    null, null);
            if (cursor != null && cursor.moveToFirst()) {
                mCardId = cursor.getInt(0);
                cursor.close();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            // it seems that content://media/external/fs_id is not accessible
            // from Android 6.0 in some phones or phone states (who knows)
            // this is an undocumented URI
        }
        return mCardId;
    }

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     */
    private void closeExternalStorageFiles() {
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
     * intent will call closeExternalStorageFiles() if the external media is
     * going to be ejected, so applications can clean up any files they have
     * open.
     */
    private void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final String action = intent.getAction();
                    if (action != null) {
                        if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                            saveQueue(true);
                            mQueueIsSaveable = false;
                            closeExternalStorageFiles();
                        } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                            mMediaMountedCount++;
                            mCardId = getCardId();
                            reloadQueue();
                            mQueueIsSaveable = true;
                            notifyChange(QUEUE_CHANGED);
                            notifyChange(META_CHANGED);
                        }
                    }
                }
            };
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, filter);
        }
    }

    private void scheduleDelayedShutdown() {
        if (mAlarmManager != null && mShutdownIntent != null) {

            if (mShutdownScheduled) {
                cancelShutdown();
            }

            if (isPlaying()) {
                LOG.info("scheduleDelayedShutdown() aborted, audio is playing.");
                mShutdownScheduled = true;
                return;
            }

            if (D) LOG.info("Scheduling shutdown in " + IDLE_DELAY + " ms");
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
            mShutdownScheduled = true;
        } else {
            mShutdownScheduled = false;
        }
    }

    private void cancelShutdown() {
        if (mAlarmManager != null && mShutdownIntent != null) {
            async(this, MusicPlaybackService::cancelShutdownTask);
        }
    }

    private static void cancelShutdownTask(MusicPlaybackService musicPlaybackService) {
        if (musicPlaybackService.mAlarmManager != null && musicPlaybackService.mShutdownIntent != null) {
            if (D)
                LOG.info("Cancelling delayed shutdown. Was it previously scheduled? : " + musicPlaybackService.mShutdownScheduled);
            try {
                musicPlaybackService.mAlarmManager.cancel(musicPlaybackService.mShutdownIntent);
                musicPlaybackService.mShutdownScheduled = false;
            } catch (Throwable ignored) {
                if (D) {
                    LOG.error(ignored.getMessage(), ignored);
                }
            }
        }
    }

    /**
     * Stops playback
     *
     * @param scheduleShutdown True to go to the idle state, false otherwise
     */
    private void stop(final boolean scheduleShutdown) {
        if (D) LOG.info("Stopping playback, scheduleShutdown = " + scheduleShutdown);
        stopPlayer();
        mFileToPlay = null;
        closeCursor();
        if (scheduleShutdown) {
            scheduleDelayedShutdown();
            updateRemoteControlClient(PLAYSTATE_STOPPED);
        } else {
            stopForeground(false);
        }
    }

    private void stopPlayer() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            LOG.info("stopPlayer()");
            mPlayer.stop();
            mIsSupposedToBePlaying = false;
        }
    }

    public void playSimple(String path) {
        String justStoppedFile = mSimplePlayerPlayingFile;
        if (mSimplePlayer != null) {
            stopSimplePlayer();
        }
        if (!path.equals(justStoppedFile)) {
            mSimplePlayer = MediaPlayer.create(this, Uri.parse(path));
            if (mSimplePlayer != null) {
                final String pathCopy = path;
                mSimplePlayerPlayingFile = path;
                mSimplePlayer.setOnCompletionListener(mp -> {
                    mSimplePlayerPlayingFile = null;
                    notifySimpleStopped(pathCopy);
                });
                mSimplePlayer.start();
            }
        }
    }

    private void stopSimplePlayer() {
        if (mSimplePlayer != null) {
            mSimplePlayer.reset();
            mSimplePlayer.release();
            notifySimpleStopped(mSimplePlayerPlayingFile);
            mSimplePlayer = null;
            mSimplePlayerPlayingFile = null;
        }
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last  The last file to be removed
     * @return the number of tracks deleted
     */
    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) {
                return 0;
            } else if (first < 0) {
                first = 0;
            } else if (last >= mPlayListLen) {
                last = mPlayListLen - 1;
            }

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= last - first + 1;
            }
            final int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) {
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            mPlayListLen -= last - first + 1;

            if (mPlayListLen < 0) {
                mPlayListLen = 0;
            }

            if (gotonext) {
                if (mPlayListLen == 0) {
                    stop(true);
                    mPlayPos = -1;
                    closeCursor();
                } else {
                    if (mShuffleEnabled) {
                        mPlayPos = getNextPosition(true, isShuffleEnabled());
                    } else if (mPlayPos >= mPlayListLen) {
                        mPlayPos = 0;
                    }
                    final boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext(() -> {
                        if (wasPlaying) {
                            play();
                        }
                    });
                }
                notifyChange(META_CHANGED);
            }
            return last - first + 1;
        }
    }

    /**
     * Adds a list to the playlist
     *
     * @param list     The list to add
     * @param position The position to place the tracks
     */
    private void addToPlayList(final long[] list, int position) {
        final int addlen = list.length;
        if (position < 0) {
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }

        if (mPlayList != null && mPlayList.length > 0) {
            final int tailsize = mPlayListLen - position;
            for (int i = tailsize; i > 0; i--) {
                if (checkBounds(position + i, mPlayList.length) &&
                        checkBounds(position + i - addlen, mPlayList.length)) {
                    mPlayList[position + i] = mPlayList[position + i - addlen];
                }
            }
            for (int i = 0; i < addlen; i++) {
                if (checkBounds(position + i, mPlayList.length) &&
                        checkBounds(i, list.length)) {
                    mPlayList[position + i] = list[i];
                }
            }
            mPlayListLen += addlen;
        }

        if (mPlayListLen == 0) {
            closeCursor();
            notifyChange(META_CHANGED);
        }
    }

    private boolean checkBounds(int i, int arrayLen) {
        return i >= 0 && i < arrayLen;
    }

    /**
     * @param trackId The track ID
     */
    private void updateCursor(final long trackId) {
        updateCursor("_id=" + trackId, null);
    }

    private void updateCursor(final String selection, final String[] selectionArgs) {
        synchronized (cursorLock) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, selection, selectionArgs);
        }
        updateAlbumCursor();
    }

    private void updateCursor(final Uri uri) {
        synchronized (cursorLock) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(uri, PROJECTION, null, null);
        }
        updateAlbumCursor();
    }

    private void updateAlbumCursor() {
        long albumId = getAlbumId();
        if (albumId >= 0) {
            mAlbumCursor = openCursorAndGoToFirst(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    ALBUM_PROJECTION, "_id=" + albumId, null);
        } else {
            mAlbumCursor = null;
        }
    }

    private Cursor openCursorAndGoToFirst(Uri uri, String[] projection,
                                          String selection, String[] selectionArgs) {
        Cursor c;

        try {
            c = getContentResolver().query(uri, projection, selection, selectionArgs, null);
        } catch (Throwable t) {
            return null;
        }

        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    private void closeCursor() {
        synchronized (cursorLock) {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            if (mAlbumCursor != null) {
                mAlbumCursor.close();
                mAlbumCursor = null;
            }
        }
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    private void openCurrentAndNext(Runnable onOpenedCallback) {
        openCurrentAndMaybeNext(true, onOpenedCallback);
    }

    private void openCurrentAndNext() {
        openCurrentAndMaybeNext(true, null);
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     *
     * @param openNext True to prepare the next track for playback, false
     *                 otherwise.
     */
    private void openCurrentAndMaybeNext(final boolean openNext, final Runnable onOpenedCallback) {
        closeCursor();
        if (mPlayListLen == 0 || mPlayList == null) {
            return;
        }
        stop(false);

        mPlayPos = Math.min(mPlayPos, mPlayList.length - 1);
        updateCursor(mPlayList[mPlayPos]);
        boolean hasOpenCursor = mCursor != null && !mCursor.isClosed();
        if (!hasOpenCursor) {
            if (openNext) {
                setNextTrack();
            }
        } else {
            final String path = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + mCursor.getLong(IDCOLIDX);
            openFile(path, new OpenFileResultCallback() {
                @Override
                public void openFileResult(boolean result) {
                    if (!result) {
                        // if we get here then opening the file failed. We can close the
                        // cursor now, because
                        // we're either going to create a new one next, or stop trying
                        closeCursor();
                        if (mOpenFailedCounter++ < 10 && mPlayListLen > 1) {
                            final int pos = getNextPosition(false, isShuffleEnabled());
                            if (scheduleShutdownAndNotifyPlayStateChange(pos)) return;
                            mPlayPos = pos;
                            stop(false);
                            mPlayPos = pos;
                            updateCursor(mPlayList[mPlayPos]);
                            boolean hasOpenCursor = mCursor != null && !mCursor.isClosed();
                            if (!hasOpenCursor) {
                                if (openNext) {
                                    setNextTrack();
                                }
                            } else {
                                final String path = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + mCursor.getLong(IDCOLIDX);
                                openFile(path, this); // try again
                            }
                        } else {
                            mOpenFailedCounter = 0;
                            LOG.warn("Failed to open file for playback");
                            scheduleDelayedShutdown();
                            if (mIsSupposedToBePlaying) {
                                mIsSupposedToBePlaying = false;
                                notifyChange(PLAYSTATE_CHANGED);
                            }
                        }
                    } else {
                        if (openNext) {
                            setNextTrack();
                        }
                        if (onOpenedCallback != null) {
                            onOpenedCallback.run();
                        }
                    }
                }
            });
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     *              otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(final boolean force, final boolean shuffleEnabled) {
        if (!force && mRepeatMode == REPEAT_CURRENT) {
            if (mPlayPos < 0) {
                return 0;
            }
            return mPlayPos;
        } else if (shuffleEnabled) {
            if (mPlayListLen <= 0) {
                return -1;
            }
            return r.nextInt(mPlayListLen);
        } else {
            if (mPlayPos >= mPlayListLen - 1) {
                if (mRepeatMode == REPEAT_NONE && !force) {
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    /**
     * Sets the track track to be played
     */
    private void setNextTrack() {
        mNextPlayPos = getNextPosition(false, isShuffleEnabled());
        if (D) LOG.info("setNextTrack: next play position = " + mNextPlayPos);

        if (mPlayer != null) {
            if (mNextPlayPos >= 0 && mPlayList != null) {
                final long id = mPlayList[mNextPlayPos];
                mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
            } else {
                mPlayer.setNextDataSource(null);
            }
        } else {
            LOG.warn("setNextTrack() -> no mPlayer instance available.");
        }
    }

    /**
     * Makes sure the playlist has enough space to hold all of the songs
     *
     * @param size The size of the playlist
     */
    private void ensurePlayListCapacity(final int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            final long[] newlist = new long[size * 2];
            final int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            if (mPlayList != null) {
                for (int i = 0; i < len; i++) {
                    newlist[i] = mPlayList[i];
                }
            }
            mPlayList = newlist;
            // FIXME: shrink the array when the needed size is much smaller
            // than the allocated size
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    private void notifyChange(final String change) {
        async(this, MusicPlaybackService::notifyChangeTask, change);
    }

    private static void notifyChangeTask(MusicPlaybackService musicPlaybackService, String change) {
        if (D) LOG.info("notifyChange: what = " + change);
        // Update the lock screen controls
        musicPlaybackService.updateRemoteControlClient(change);
        if (POSITION_CHANGED.equals(change)) {
            return;
        }
        final Intent intent = new Intent(change);
        long audioId = musicPlaybackService.getAudioId();
        String artistName = musicPlaybackService.getArtistName();
        String albumName = musicPlaybackService.getAlbumName();
        String trackName = musicPlaybackService.getTrackName();
        boolean isPlaying = musicPlaybackService.isPlaying();
        boolean favorite = musicPlaybackService.isFavorite();
        intent.putExtra("id", audioId);
        intent.putExtra("artist", artistName);
        intent.putExtra("album", albumName);
        intent.putExtra("track", trackName);
        intent.putExtra("playing", isPlaying);
        intent.putExtra("isfavorite", favorite);
        final Intent musicIntent = new Intent(intent);
        musicIntent.setAction(change.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
        if (QUEUE_CHANGED.equals(change)) {
            musicPlaybackService.saveQueue(true);
            if (isPlaying) {
                musicPlaybackService.setNextTrack();
            }
        } else {
            musicPlaybackService.saveQueue(false);
        }
    }

    /**
     * Notify the change-receivers that simple player has stopped
     */
    private void notifySimpleStopped(final String path) {
        if (D) LOG.info("notifySimplePlayerStopped");

        final Intent intent = new Intent(SIMPLE_PLAYSTATE_STOPPED);
        intent.putExtra("path", path);
    }

    /**
     * Updates the lock screen controls.
     *
     * @param what The broadcast
     */
    private void updateRemoteControlClient(final String what) {
        if (mRemoteControlClient == null) {
            LOG.info("mRemoteControlClient is null, review your logic");
            return;
        }

        int playState;
        if (isPlaying()) {
            playState = RemoteControlClient.PLAYSTATE_PLAYING;
        } else {
            playState = RemoteControlClient.PLAYSTATE_PAUSED;
            if (what.equals(PLAYSTATE_STOPPED)) {
                playState = RemoteControlClient.PLAYSTATE_STOPPED;
            }
        }

        if (what == null) {
            return;
        }

        switch (what) {
            case PLAYSTATE_CHANGED:
            case POSITION_CHANGED:
            case PLAYSTATE_STOPPED:
                async(mRemoteControlClient, MusicPlaybackService::remoteControlClientSetPlaybackStateTask, playState);
                break;
            case META_CHANGED:
        }
    }

    private static void remoteControlClientSetPlaybackStateTask(RemoteControlClient rc, int playState) {
        try {
            rc.setPlaybackState(playState);
        } catch (Throwable throwable) {
            // rare android internal NPE
        }
    }

    /**
     * Saves the queue
     *
     * @param full True if the queue is full
     */
    private void saveQueue(final boolean full) {
        if (!mQueueIsSaveable || mPreferences == null) {
            return;
        }

        final SharedPreferences.Editor editor = mPreferences.edit();
        if (full) {
            final StringBuilder q = new StringBuilder();
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int) (n & 0xf);
                        n >>>= 4;
                        q.append(HEX_DIGITS[digit]);
                    }
                    q.append(";");
                }
            }
            editor.putString("queue", q.toString());
            editor.putInt("cardid", mCardId);
        }
        editor.putInt("curpos", mPlayPos);
        if (mPlayer != null && mPlayer.isInitialized()) {
            try {
                final long pos = mPlayer.position();
                editor.putLong("seekpos", pos);
            } catch (Throwable e) {
                // usually an IllegalStateException coming
                // from zig.zak.media.tor.MusicPlaybackService$MultiPlayer.position
                // which comes from a native call to MediaPlayer.getCurrentPosition()
            }
        }
        editor.putInt("repeatmode", mRepeatMode);
        editor.putBoolean("shufflemode", mShuffleEnabled);
        editor.apply();
    }

    /**
     * Reloads the queue as the user left it the last time they stopped using
     * Apollo
     */
    private void reloadQueue() {
        String q = null;
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            q = mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                final char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += c - '0' << shift;
                    } else if (c >= 'a' && c <= 'f') {
                        n += 10 + c - 'a' << shift;
                    } else {
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;
            final int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;
            updateCursor(mPlayList[mPlayPos]);
            if (mCursor == null) {
                SystemClock.sleep(3000);
                try {
                    // TODO: well, this is garbage, since
                    // there is a 3 seconds sleep, all sort
                    // of things could happen to the mutable
                    // variable mPlayPos, including set it to -1
                    // this need to be recoded
                    updateCursor(mPlayList[mPlayPos]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ignore and return
                    return;
                }
            }
            synchronized (this) {
                closeCursor();
                mOpenFailedCounter = 20;
                openCurrentAndNext();
            }
            if (mPlayer == null || !mPlayer.isInitialized()) {
                mPlayListLen = 0;
                return;
            }

            final long seekpos = mPreferences.getLong("seekpos", 0);
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);

            if (D) {
                LOG.info("restored queue, currently at position "
                        + position() + "/" + duration()
                        + " (requested " + seekpos + ")");
            }

            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;
        }
    }

    interface OpenFileResultCallback {
        void openFileResult(boolean result);
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public void openFile(final String path, final OpenFileResultCallback callback) {
        if (D) LOG.info("openFile: path = " + path);
        if (callback == null) {
            throw new IllegalArgumentException("MusicPlaybackService.openFile requires a non null OpenFileResultCallback argument");
        }
        synchronized (this) {
            if (path == null) {
                callback.openFileResult(false);
                return;
            }

            // If mCursor is null, try to associate path with a database cursor
            if (mCursor == null) {
                Uri uri = Uri.parse(path);
                long id = -1;
                try {
                    id = Long.valueOf(uri.getLastPathSegment());
                } catch (NumberFormatException ex) {
                    // Ignore
                }

                if (id != -1 && path.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    updateCursor(uri);
                } else if (id != -1 && path.startsWith(MediaStore.Files.getContentUri("external").toString())) {
                    updateCursor(id);
                } else {
                    String where = MediaStore.Audio.Media.DATA + "=?";
                    String[] selectionArgs = new String[]{path};
                    updateCursor(where, selectionArgs);
                }
                try {
                    if (mCursor != null) {
                        ensurePlayListCapacity(1);
                        mPlayListLen = 1;
                        mPlayList[0] = mCursor.getLong(IDCOLIDX);
                        mPlayPos = 0;
                    }
                } catch (UnsupportedOperationException e) {
                    LOG.error("Error while opening file for play", e);
                    callback.openFileResult(false);
                    return;
                } catch (StaleDataException | IllegalStateException e) {
                    LOG.error("Error with database cursor while opening file for play", e);
                    callback.openFileResult(false);
                    return;
                }
            }
            mFileToPlay = path;
            if (mPlayer != null) { // machine state issues in general with original Apollo code
                mPlayer.setDataSource(mFileToPlay, () -> {
                    if (mPlayer != null && mPlayer.isInitialized()) {
                        mOpenFailedCounter = 0;
                        callback.openFileResult(true);
                    } else {
                        stop(true);
                        callback.openFileResult(false);
                    }

                });
            } else {
                stop(true);
                callback.openFileResult(false);
            }
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    private int getAudioSessionId() {
        if (mPlayer == null) {
            return -1;
        }

        synchronized (audioSessionIdLock) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * Indicates if the media storage device has been mounted or not
     *
     * @return 1 if Intent.ACTION_MEDIA_MOUNTED is called, 0 otherwise
     */
    private int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the shuffle mode
     *
     * @return The current shuffle mode (all, party, none)
     */
    private boolean isShuffleEnabled() {
        return mShuffleEnabled;
    }

    /**
     * Returns the repeat mode
     *
     * @return The current repeat mode (all, one, none)
     */
    private int getRepeatMode() {
        return mRepeatMode;
    }

    /**
     * Removes all instances of the track with the given ID from the playlist.
     *
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    private int removeTrack(final long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlayListLen; i++) {
                if (mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last  The last file to be removed
     * @return the number of tracks deleted
     */
    private int removeTracks(final int first, final int last) {
        final int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Returns the position in the queue
     *
     * @return the current position in the queue
     */
    private int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }

    /**
     * Returns the path to current song
     *
     * @return The path to the current song
     */
    public String getPath() {
        synchronized (this) {
            if (mCursor == null || mCursor.isClosed()) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.DATA));
        }
    }

    /**
     * Returns the album name
     *
     * @return The current song album Name
     */
    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null || mCursor.isClosed()) {
                return null;
            }
            try {
                return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
            } catch (Throwable e) {
                e.printStackTrace();
                return "---";
            }
        }
    }

    /**
     * Returns the song name
     *
     * @return The current song name
     */
    private String getTrackName() {
        synchronized (this) {
            if (mCursor == null || mCursor.isClosed()) {
                return null;
            }
            try {
                return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.TITLE));
            } catch (Throwable e) {
                e.printStackTrace();
                return "---";
            }
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    public String getArtistName() {
        try {
            synchronized (this) {
                if (mCursor == null || mCursor.isClosed()) {
                    return null;
                }
                return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
            }
        } catch (Throwable e) {
            // what else
            return "";
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    private String getAlbumArtistName() {
        try {
            synchronized (this) {
                if (mAlbumCursor == null || mAlbumCursor.isClosed()) {
                    return null;
                }
                return mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));
            }
        } catch (Throwable e) {
            // avoid crash due to IllegalStateException, or any other exception
            return "";
        }
    }

    /**
     * Returns the album ID
     *
     * @return The current song album ID
     */
    public long getAlbumId() {
        synchronized (this) {
            try {
                if (mCursor == null || mCursor.isClosed()) {
                    return -1;
                }
                return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
            } catch (IllegalStateException | StaleDataException e) {
                // this is what happens when a cursor is stored as a state
                LOG.error("Error using db cursor to get album id", e);
                mCursor = null;
                return -1;
            }
        }
    }

    /**
     * Returns the artist ID
     *
     * @return The current song artist ID
     */
    private long getArtistId() {
        synchronized (this) {
            if (mCursor == null || mCursor.isClosed()) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST_ID));
        }
    }

    /**
     * Returns the current audio ID
     *
     * @return The current track ID
     */
    private long getAudioId() {
        if (mPlayList != null &&
                mPlayPos >= 0 &&
                mPlayPos < mPlayList.length &&
                mPlayer != null && mPlayer.isInitialized()) {
            try {
                return mPlayList[mPlayPos];
            } catch (IndexOutOfBoundsException ioob) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns the current audio for simple player ID
     *
     * @return The current simple player track ID
     */
    private long getCurrentSimplePlayerAudioId() {
        synchronized (this) {
            long id = -1;
            if (mSimplePlayerPlayingFile != null) {
                id = getIdFromPath(mSimplePlayerPlayingFile, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
                if (id == -1) {
                    id = getIdFromPath(mSimplePlayerPlayingFile, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                }
            }
            return id;
        }
    }

    /**
     * Returns the current id of file at given path in Selected uri or -1 if not found
     *
     * @return File id
     */
    private long getIdFromPath(String path, Uri uri) {
        String selectionClause = MediaStore.Audio.Media.DATA + " = ?";
        String[] selectionArgs = {path};
        Cursor cursor = getContentResolver().query(uri, SIMPLE_PROJECTION, selectionClause, selectionArgs, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            long id = cursor.getLong(IDCOLIDX);
            cursor.close();
            return id;
        }
        return -1;
    }


    /**
     * Seeks the current track to a specific time
     *
     * @param position The time to seek to
     * @return The time to play the track at
     */
    public long seek(long position) {
        if (mPlayer != null && mPlayer.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else if (position > mPlayer.duration()) {
                position = mPlayer.duration();
            }
            long result = mPlayer.seek(position);
            notifyChange(POSITION_CHANGED);
            return result;
        }
        return -1;
    }

    /**
     * Returns the current position in time of the current track
     *
     * @return The current playback position in milliseconds
     */
    public long position() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Expensive method, do not use in main thread.
     * <p>
     * Returns the full duration of the current track
     *
     * @return The duration of the current track in milliseconds
     */
    public long duration() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the queue
     *
     * @return The queue as a long[]
     */
    public long[] getQueue() {
        synchronized (this) {
            if (mPlayList == null) {
                return new long[0];
            }
            final long[] list = new long[mPlayListLen];
            System.arraycopy(mPlayList, 0, list, 0, mPlayListLen);
            return list;
        }
    }

    /**
     * @return True if music is playing, false otherwise
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /**
     * This is not the same as being paused. This means there's no track loaded.
     *
     * @return True if there's no track loaded.
     */
    public boolean isStopped() {
        return mCursor == null || mCursor.isClosed();
    }

    /**
     * True if the current track is a "favorite", false otherwise
     */
    public boolean isFavorite() {
        return false;
    }

    /**
     * Opens a list for playback
     *
     * @param list     The list of tracks to open
     * @param position The position to start playback at
     */
    public void open(final long[] list, final int position) {
        launchPlayerActivity = true;
        synchronized (this) {
            final long oldId = getAudioId();
            final int listlength = list.length;
            boolean newlist = true;
            if (mPlayListLen == listlength) {
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlayList[i]) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(QUEUE_CHANGED);
            }
            if (position == -1) {
                mPlayPos = 0;
            }
            if (position >= 0) {
                mPlayPos = position;
            }
            mHistory.clear();
            openCurrentAndNext(() -> {
                if (oldId != getAudioId()) {
                    play();
                    notifyChange(META_CHANGED);
                }
            });
        }
    }

    /**
     * Stops playback and schedules the service to shutdown later unless it's playing audio.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Resumes or starts playback.
     */
    public void play() {
        if (mAudioManager == null) {
            return;
        }

        stopSimplePlayer();


        if (mPlayer != null && mPlayer.isInitialized()) {
            setNextTrack();

            if (mShuffleEnabled && (mHistory.empty() || mHistory.peek() != mPlayPos)) {
                mHistory.push(mPlayPos);
            }

            final long duration = mPlayer.duration();
            if (mRepeatMode != REPEAT_CURRENT && duration > 2000
                    && mPlayer.position() >= duration - 2000) {
                gotoNext(true);
            }

            mPlayer.start();
            if (!mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = true;
                notifyChange(PLAYSTATE_CHANGED);
            }
            cancelShutdown();
        }
    }

    /**
     * Temporarily pauses playback.
     */
    public void pause() {
        if (D) LOG.info("Pausing playback");
        synchronized (this) {
            if (mIsSupposedToBePlaying && mPlayer != null) {
                mPlayer.pause();
                scheduleDelayedShutdown();
                mIsSupposedToBePlaying = false;
                if (musicPlaybackActivityInForeground) {
                    updateRemoteControlClient(PLAYSTATE_STOPPED);
                } else {
                    notifyChange(PLAYSTATE_CHANGED);
                }
            }
        }
    }

    /**
     * Changes from the current track to the next track
     *
     * @param force -> set to true when gotoNext is invoked by an user action
     */
    public void gotoNext(final boolean force) {
        if (D) LOG.info("Going to next track");

        if (force && mRepeatMode == REPEAT_CURRENT) {
            setRepeatMode(REPEAT_ALL);
        }

        synchronized (this) {
            if (mPlayListLen <= 0) {
                if (D) LOG.info("No play queue");
                scheduleDelayedShutdown();
                return;
            }
            final int pos = getNextPosition(force, isShuffleEnabled());
            if (scheduleShutdownAndNotifyPlayStateChange(pos)) return;
            mPlayPos = pos;
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext(() -> {
                play();
                notifyChange(META_CHANGED);
            });
        }
    }

    private boolean scheduleShutdownAndNotifyPlayStateChange(int pos) {
        if (pos < 0) {
            scheduleDelayedShutdown();
            if (mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
            }
            return true;
        }
        return false;
    }

    /**
     * Changes from the current track to the previous played track
     */
    public void prev() {
        if (D) LOG.info("Going to previous track");

        if (mRepeatMode == REPEAT_CURRENT) {
            setRepeatMode(REPEAT_ALL);
        }

        synchronized (this) {
            if (!mShuffleEnabled) {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayListLen - 1;
                }
            } else {
                if (!mHistory.empty()) {
                    mHistory.pop();
                    if (!mHistory.empty()) {
                        mPlayPos = mHistory.peek();
                    }
                }
            }
        }

        stop(false);
        openCurrent(() -> {
            play();
            notifyChange(META_CHANGED);
        });
    }

    /**
     * We don't want to open the current and next track when the user is using
     * the {@code #prev()} method because they won't be able to travel back to
     * the previously listened track if they're shuffling.
     */
    private void openCurrent(Runnable postOpenCallback) {
        openCurrentAndMaybeNext(false, postOpenCallback);
    }

    /**
     * Sets the repeat mode
     *
     * @param repeatMode The repeat mode to use
     */
    public void setRepeatMode(final int repeatMode) {
        synchronized (this) {
            mRepeatMode = repeatMode;
            setNextTrack();
            saveQueue(false);
            notifyChange(REPEATMODE_CHANGED);
        }
        Asyncs.async(MusicPlaybackService::saveLastRepeatStateAsync, repeatMode);
    }

    private static void saveLastRepeatStateAsync(int repeatMode) {
        ConfigurationManager CM = ConfigurationManager.instance();
        CM.setInt(Constants.PREF_KEY_GUI_PLAYER_REPEAT_MODE, repeatMode);
    }

    /**
     * Sets the shuffle mode
     *
     * @param on The shuffle mode to use
     */
    public void enableShuffle(boolean on) {
        mShuffleEnabled = on;
        Asyncs.async(MusicPlaybackService::saveLastShuffleStateAsync, on);
        notifyChange(SHUFFLEMODE_CHANGED);
    }

    private static void saveLastShuffleStateAsync(boolean shuffleEnabled) {
        ConfigurationManager CM = ConfigurationManager.instance();
        CM.setBoolean(Constants.PREF_KEY_GUI_PLAYER_SHUFFLE_ENABLED, shuffleEnabled);
    }

    /**
     * Sets the position of a track in the queue
     *
     * @param index The position to place the track
     */
    public void setQueuePosition(final int index) {
        synchronized (this) {
            stop(false);
            mPlayPos = index;
            openCurrentAndNext(() -> {
                play();
                notifyChange(META_CHANGED);
            });
        }
    }

    /**
     * Queues a new list for playback
     *
     * @param list   The list to queue
     * @param action The action to take
     */
    public void enqueue(final long[] list, final int action) {
        synchronized (this) {
            if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
            } else {
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayListLen - list.length;
                    openCurrentAndNext(() -> {
                        play();
                        notifyChange(META_CHANGED);
                    });
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
            }
            if (!isPlaying()) {
                openCurrentAndNext(() -> {
                    play();
                    pause();
                    notifyChange(META_CHANGED);
                });
            }
        }
    }

    /**
     * Cycles through the different repeat modes
     */
    private void cycleRepeat() {
        if (mRepeatMode == REPEAT_NONE) {
            setRepeatMode(REPEAT_ALL);
        } else if (mRepeatMode == REPEAT_ALL) {
            setRepeatMode(REPEAT_CURRENT);
        } else {
            setRepeatMode(REPEAT_NONE);
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public void refresh() {
        notifyChange(REFRESH);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            handleCommandIntent(intent);
        }
    };

    // TODO: Check why this isn't being used anywhere. Perhaps should be called in EngineService's shutdown logic
    public static void stopService(Context context) {
        LOG.info("stopService() <static>");
        Intent i = new Intent();
        i.setClass(context, MusicPlaybackService.class);
        context.stopService(i);
    }

    private static final class MusicPlayerHandler extends Handler {
        private final WeakReference<MusicPlaybackService> mService;
        private float mCurrentVolume = 1.0f;

        /**
         * Constructor of <code>MusicPlayerHandler</code>
         *
         * @param service The service to use.
         * @param looper  The thread to run on.
         */
        public MusicPlayerHandler(final MusicPlaybackService service, final Looper looper) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            final MusicPlaybackService service = mService.get();
            if (service == null) {
                return;
            }

            switch (msg.what) {
                case FADE_DOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        sendEmptyMessageDelayed(FADE_DOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;
                case FADE_UP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        sendEmptyMessageDelayed(FADE_UP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    service.mPlayer.setVolume(mCurrentVolume);
                    break;
                case SERVER_DIED:
                    if (service.isPlaying()) {
                        service.gotoNext(true);
                    } else {
                        service.openCurrentAndNext();
                    }
                    break;
                case TRACK_WENT_TO_NEXT:
                    if (service.mNextPlayPos == -1) {
                        service.mNextPlayPos = 0;
                    }
                    service.mPlayPos = service.mNextPlayPos;
                    if (service.mCursor != null) {
                        service.mCursor.close();
                    }

                    if (service.mPlayPos < service.mPlayList.length) {
                        service.updateCursor(service.mPlayList[service.mPlayPos]);
                        service.notifyChange(META_CHANGED);
                        service.setNextTrack();
                    }
                    break;
                case TRACK_ENDED:
                    if (service.mRepeatMode == REPEAT_CURRENT) {
                        service.seek(0);
                        service.play();
                    } else {
                        service.gotoNext(false);
                    }
                    break;
                case RELEASE_WAKELOCK:
                    service.mWakeLock.release();
                    break;
                case FOCUS_CHANGE:
                    if (D) LOG.info("Received audio focus change event " + msg.arg1);
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (service.isPlaying()) {
                                service.mPausedByTransientLossOfFocus =
                                        msg.arg1 == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                            }
                            service.pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            removeMessages(FADE_UP);
                            sendEmptyMessage(FADE_DOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (!service.isPlaying()
                                    && service.mPausedByTransientLossOfFocus) {
                                service.mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                service.mPlayer.setVolume(mCurrentVolume);
                                service.play();
                            } else {
                                removeMessages(FADE_DOWN);
                                sendEmptyMessage(FADE_UP);
                            }
                            break;
                        default:
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void mediaPlayerAsyncAction(MediaPlayer mediaPlayer, MediaPlayerAction action) {
        if (mediaPlayer != null) {
            async(mediaPlayer, MusicPlaybackService::mediaPlayerAction, action);
        }
    }

    enum MediaPlayerAction {
        START,
        RELEASE,
        RESET
    }

    private static void mediaPlayerAction(MediaPlayer mediaPlayer, MediaPlayerAction action) {
        try {
            switch (action) {
                case START:
                    mediaPlayer.start();
                    return;
                case RELEASE:
                    mediaPlayer.release();
                    return;
                case RESET:
                    mediaPlayer.reset();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static final class MultiPlayer implements MediaPlayer.OnErrorListener,
            MediaPlayer.OnCompletionListener {

        private static final String TAG = MultiPlayer.class.getSimpleName();
        private final WeakReference<MusicPlaybackService> mService;

        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();

        private MediaPlayer mNextMediaPlayer;

        private Handler mHandler;

        private boolean mIsInitialized = false;

        private interface OnPlayerPrepareCallback {
            void onPrepared(boolean result);
        }

        /**
         * Constructor of <code>MultiPlayer</code>
         */
        public MultiPlayer(final MusicPlaybackService service) {
            mService = new WeakReference<>(service);
            mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        }

        /**
         * @param path The path of the file, or the http/rtsp URL of the stream
         *             you want to play
         */
        public void setDataSource(final String path, final Runnable callback) {
            setDataSourceImpl(mCurrentMediaPlayer, path, result -> {
                mIsInitialized = result;
                if (mIsInitialized) {
                    setNextDataSource(null);
                }
                if (callback != null) {
                    callback.run();
                }
            });
        }

        /**
         * @param player The {@link MediaPlayer} to use
         * @param path   The path of the file, or the http/rtsp URL of the stream
         *               you want to play
         * @return True if the <code>player</code> has been prepared and is
         * ready to play, false otherwise
         */
        private void setDataSourceImpl(MediaPlayer player, String path, OnPlayerPrepareCallback callback) {
            if (Ref.alive(mService)) {
                MusicPlaybackService musicPlaybackService = mService.get();
                async(musicPlaybackService,
                        MusicPlaybackService.MultiPlayer::setDataSourceTask,
                        player, path, callback, this);
            }
        }

        private static void setDataSourceTask(MusicPlaybackService mService,
                                              MediaPlayer player, String path,
                                              OnPlayerPrepareCallback callback,
                                              MultiPlayer multiPlayer) {
            try {
                player.reset();
                if (path.startsWith("content://")) {
                    player.setDataSource(mService, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (Throwable e) {
                // TODO: notify the user why the file couldn't be opened due to an unknown error
                callback.onPrepared(false);
                return;
            }
            player.setOnCompletionListener(multiPlayer);
            player.setOnErrorListener(multiPlayer);
            final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mService.getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mService.getPackageName());
            mService.sendBroadcast(intent);
            callback.onPrepared(true);
        }

        /**
         * Set the MediaPlayer to start when this MediaPlayer finishes playback.
         *
         * @param path The path of the file, or the http/rtsp URL of the stream
         *             you want to play
         */
        public void setNextDataSource(final String path) {
            try {
                mCurrentMediaPlayer.setNextMediaPlayer(null);
            } catch (IllegalArgumentException e) {
                LOG.info("Next media player is current one, continuing");
            } catch (IllegalStateException e) {
                LOG.error("Media player not initialized!");
                return;
            } catch (Throwable e) {
                LOG.error("Media player fatal error", e);
                return;
            }
            releaseNextMediaPlayer();
            if (path == null) {
                return;
            }
//            mCurrentMediaPlayer = new MediaPlayer();
//            try {
//                mCurrentMediaPlayer.setDataSource(mService.get(), Uri.parse(path));
//                mCurrentMediaPlayer.prepare();
//                mCurrentMediaPlayer.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            initNextMediaPlayer();

            setDataSourceImpl(mNextMediaPlayer, path, result -> {
                Log.i(TAG, "path=>" + path);
                if (result) {
                    try {
                        mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
                    } catch (Throwable e) {
                        LOG.error("Media player fatal error", e);
                    }
                } else {
                    releaseNextMediaPlayer();
                }
            });
        }

        private void initNextMediaPlayer() {
            if (Ref.alive(mService)) {
                mNextMediaPlayer = new MediaPlayer();
                mNextMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                try {
                    mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
                } catch (Throwable e) {
                    LOG.error("Media player Illegal State exception", e);
                }
            }
        }

        private void releaseCurrentMediaPlayer() {
            if (mCurrentMediaPlayer != null) {
                try {
                    mediaPlayerAsyncAction(mCurrentMediaPlayer, MediaPlayerAction.RELEASE);
                } catch (Throwable e) {
                    LOG.warn("releaseCurrentMediaPlayer() couldn't release mCurrentMediaPlayer", e);
                } finally {
                    mCurrentMediaPlayer = null;
                }
            }
        }

        private void releaseNextMediaPlayer() {
            if (mNextMediaPlayer != null) {
                try {
                    mediaPlayerAsyncAction(mNextMediaPlayer, MediaPlayerAction.RELEASE);
                } catch (Throwable e) {
                    LOG.warn("releaseNextMediaPlayer() couldn't release mNextMediaPlayer", e);
                } finally {
                    mNextMediaPlayer = null;
                }
            }
        }

        /**
         * Sets the handler
         *
         * @param handler The handler to use
         */
        public void setHandler(final Handler handler) {
            mHandler = handler;
        }

        /**
         * @return True if the player is ready to go, false otherwise
         */
        public boolean isInitialized() {
            return mIsInitialized;
        }

        /**
         * Starts or resumes playback.
         */
        public void start() {
            Log.i(TAG, "start");
            if (mCurrentMediaPlayer != null) {
                try {
                    mediaPlayerAsyncAction(mCurrentMediaPlayer, MediaPlayerAction.START);
                } catch (Throwable ignored) {
                }
            }
        }

        /**
         * Resets the MediaPlayer to its uninitialized state.
         */
        public void stop() {
            if (mCurrentMediaPlayer != null) {
                try {
                    mediaPlayerAsyncAction(mCurrentMediaPlayer, MediaPlayerAction.RESET);
                    mIsInitialized = false;
                } catch (Throwable t) {
                    // recover from possible IllegalStateException caused by native _reset() method.
                }
            }
        }


        /**
         * Releases resources associated with this MediaPlayer object.
         */
        public void release() {
            stop();
            if (mCurrentMediaPlayer != null) {
                try {
                    mediaPlayerAsyncAction(mCurrentMediaPlayer, MediaPlayerAction.RELEASE);
                } catch (Throwable ignored) {
                }
            }
        }

        /**
         * Pauses playback. Call start() to resume.
         */
        public void pause() {
            if (mCurrentMediaPlayer != null) {
                try {
                    mCurrentMediaPlayer.pause();
                } catch (Throwable ignored) {
                }
            }
        }

        /**
         * NOTE: This method can take a long time, do not use on the main thread
         * Gets the duration of the file.
         *
         * @return The duration in milliseconds
         */
        public long duration() {
            if (mCurrentMediaPlayer != null) {
                try {
                    return mCurrentMediaPlayer.getDuration();
                } catch (Throwable t) {
                    return -1;
                }
            }
            return -1;
        }

        /**
         * Gets the current playback position.
         *
         * @return The current position in milliseconds
         */
        public long position() {
            long result = 0;
            if (mCurrentMediaPlayer != null) {
                try {
                    result = mCurrentMediaPlayer.getCurrentPosition();
                } catch (Throwable ignored) {
                }
            }
            return result;
        }

        /**
         * Gets the current playback position.
         *
         * @param whereto The offset in milliseconds from the start to seek to
         * @return The offset in milliseconds from the start to seek to
         */
        public long seek(final long whereto) {
            if (mCurrentMediaPlayer != null) {
                try {
                    mCurrentMediaPlayer.seekTo((int) whereto);
                } catch (Throwable ignored) {
                }
            }
            return whereto;
        }

        /**
         * Sets the volume on this player.
         *
         * @param vol Left and right volume scalar
         */
        public void setVolume(final float vol) {
            if (mCurrentMediaPlayer != null) {
                try {
                    mCurrentMediaPlayer.setVolume(vol, vol);
                } catch (Throwable t) {
                    // possible native IllegalStateException.
                }
            }
        }

        /**
         * Sets the audio session ID.
         *
         * @param sessionId The audio session ID
         */
        public void setAudioSessionId(final int sessionId) {
            if (mCurrentMediaPlayer != null) {
                try {
                    mCurrentMediaPlayer.setAudioSessionId(sessionId);
                } catch (Throwable ignored) {
                }
            }
        }

        /**
         * Returns the audio session ID.
         *
         * @return The current audio session ID.
         */
        public int getAudioSessionId() {
            int result = 0;
            if (mCurrentMediaPlayer != null) {
                try {
                    result = mCurrentMediaPlayer.getAudioSessionId();
                } catch (Throwable ignored) {
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onError(final MediaPlayer mp, final int what, final int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    try {
                        mIsInitialized = false;
                        releaseCurrentMediaPlayer();
                        mCurrentMediaPlayer = new MediaPlayer();
                        mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                    } catch (Throwable ignored) {
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onCompletion(final MediaPlayer mp) {
            try {
                if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                    releaseCurrentMediaPlayer();
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    mNextMediaPlayer = null;
                    mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    mService.get().mWakeLock.acquire(30000);
                    mHandler.sendEmptyMessage(TRACK_ENDED);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public void shutdown() {
        mPlayPos = -1;
        stopForeground(true);
        if (mShutdownIntent != null) {
            cancelShutdown();
        }
        MusicUtils.requestMusicPlaybackServiceShutdown(this);
    }
}
