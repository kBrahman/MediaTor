package z.zer.tor.media.utils;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.view.Menu;
import android.view.SubMenu;

import z.zer.tor.media.MusicPlaybackService;
import z.zer.tor.media.loaders.FavoritesLoader;
import z.zer.tor.media.loaders.LastAddedLoader;
import z.zer.tor.media.loaders.PlaylistLoader;
import z.zer.tor.media.loaders.SongLoader;
import z.zer.tor.media.menu.FragmentMenuItems;
import z.zer.tor.media.model.Playlist;
import z.zer.tor.media.model.Song;
import z.zer.tor.media.provider.FavoritesStore.FavoriteColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import service.IApolloService;
import z.zer.tor.media.R;
import z.zer.tor.media.util.Logger;

/**
 * A collection of helpers directly related to music or Apollo's service.
 */
public final class MusicUtils {

    private static final Logger LOG = Logger.getLogger(MusicUtils.class);

    public static IApolloService musicPlaybackService = null;

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap;

    private static final long[] sEmptyList;

    private static ContentValues[] mContentValuesCache = null;

    static {
        mConnectionMap = new WeakHashMap<>();
        sEmptyList = new long[0];
    }

    /* This class is never initiated */
    public MusicUtils() {
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper mContextWrapper = token.mWrappedContext;
        try {
            final ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
            if (mBinder == null) {
                return;
            }
            mContextWrapper.unbindService(mBinder);
            if (mConnectionMap.isEmpty()) {
                musicPlaybackService = null;
            }
        } catch (Throwable ignored) {
            LOG.error(ignored.getMessage(), ignored);
        }
    }

    public static void requestMusicPlaybackServiceShutdown(Context context) {
        if (context == null) {
            LOG.warn("requestMusicPlaybackServiceShutdown() aborted. context is null.");
            return;
        }
        try {
            final Intent shutdownIntent = new Intent(context, MusicPlaybackService.class);
            shutdownIntent.setAction(MusicPlaybackService.SHUTDOWN_ACTION);
            shutdownIntent.putExtra("force", true);
            LOG.info("MusicUtils.requestMusicPlaybackServiceShutdown() -> sending shut down intent now");
            LOG.info("MusicUtils.requestMusicPlaybackServiceShutdown() -> " + shutdownIntent);
            context.startService(shutdownIntent);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isPaused() {
        return !MusicUtils.isPlaying() && !MusicUtils.isStopped();
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;

        /**
         * Constructor of <code>ServiceBinder</code>
         *
         * @param callback The {@link ServiceConnection} to use
         */
        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            musicPlaybackService = IApolloService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            musicPlaybackService = null;
        }
    }

    public static final class ServiceToken {
        public ContextWrapper mWrappedContext;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The {@link ContextWrapper} to use
         */
        public ServiceToken(final ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context   The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number    The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     * albums, songs, genres, and playlists.
     */
    public static String makeLabel(final Context context, final int pluralInt,
                                   final int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs    The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    public static String makeTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs -= hours * 3600;
        mins = secs / 60;
        secs -= mins * 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        try {
            if (musicPlaybackService != null) {
                musicPlaybackService.next();
            }
        } catch (final RemoteException ignored) {
        }
    }

    public static void previous(final Context context) {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    /**
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            // TODO: Check for PHONE_STATE Permissions here.
            if (musicPlaybackService != null) {
                if (musicPlaybackService.isPlaying()) {
                    musicPlaybackService.pause();
                } else {
                    musicPlaybackService.play();
                }
            }
        } catch (final Exception ignored) {
        }
    }

    /**
     * Gets back to playing whatever it was playing before.
     */
    public static void play() {
        if (musicPlaybackService != null) {
            try {
                musicPlaybackService.play();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void pause() {
        if (musicPlaybackService != null) {
            try {
                musicPlaybackService.pause();
            } catch (Throwable ignored) {

            }
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            if (musicPlaybackService != null) {
                switch (musicPlaybackService.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        musicPlaybackService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;
                    case MusicPlaybackService.REPEAT_ALL:
                        musicPlaybackService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        break;
                    default:
                        musicPlaybackService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        if (musicPlaybackService != null) {
            try {
                musicPlaybackService.enableShuffle(!isShuffleEnabled());
            } catch (RemoteException ignored) {
            }
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.isPlaying();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    public static boolean isStopped() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.isStopped();
            } catch (final RemoteException ignored) {
            }
        }
        return true;
    }

    /**
     * @return The current shuffle mode.
     */
    public static boolean isShuffleEnabled() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.isShuffleEnabled();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    /**
     * @return The current repeat mode.
     */
    public static int getRepeatMode() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getRepeatMode();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The current track name.
     */
    public static String getTrackName() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getTrackName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current artist name.
     */
    public static String getArtistName() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getArtistName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current album name.
     */
    public static String getAlbumName() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getAlbumName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current album Id.
     */
    public static long getCurrentAlbumId() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getAlbumId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The current song Id.
     */
    public static long getCurrentAudioId() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getAudioId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The current song Id played by Simple Player.
     */
    public static long getCurrentSimplePlayerAudioId() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getCurrentSimplePlayerAudioId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The current artist Id.
     */
    public static long getCurrentArtistId() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getArtistId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The audio session Id.
     */
    public static int getAudioSessionId() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.getAudioSessionId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The queue.
     */
    public static long[] getQueue() {
        try {
            if (musicPlaybackService != null) {
                return musicPlaybackService.getQueue();
            }
        } catch (final RemoteException ignored) {
        }
        return sEmptyList;
    }

    /**
     * @param id The ID of the track to remove.
     * @return removes track from a playlist or the queue.
     */
    public static int removeTrack(final long id) {
        try {
            if (musicPlaybackService != null) {
                return musicPlaybackService.removeTrack(id);
            }
        } catch (final RemoteException ignored) {
        }
        return 0;
    }

    /**
     * @return The position of the current track in the queue.
     */
    public static int getQueuePosition() {
        try {
            if (musicPlaybackService != null) {
                return musicPlaybackService.getQueuePosition();
            }
        } catch (final RemoteException ignored) {
        }
        return 0;
    }

    /**
     * @param cursor The {@link Cursor} used to perform our query.
     * @return The song list for a MIME type.
     */
    public static long[] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final long[] list = new long[len];
        cursor.moveToFirst();
        int columnIndex;
        try {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (final IllegalArgumentException notaplaylist) {
            columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(columnIndex);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public static Song getSong(Context context, final long songId) {
        final StringBuilder mSelection = new StringBuilder(BaseColumns._ID + "=?");
        mSelection.append(" AND " + AudioColumns.IS_MUSIC + "=1");
        mSelection.append(" AND " + AudioColumns.TITLE + " != ''"); //$NON-NLS-2$

        final Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        AudioColumns.ARTIST,
                        /* 3 */
                        AudioColumns.ALBUM,
                        /* 4 */
                        AudioColumns.DURATION
                },
                mSelection.toString(),
                new String[]{String.valueOf(songId)},
                PreferenceUtils.getInstance().getSongSortOrder());

        if (cursor != null && cursor.getCount() == 1) {
            cursor.moveToFirst();
            return new Song(songId, cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4));
        } else {
            return null;
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the artist.
     * @return The song list for an artist.
     */
    public static long[] getSongListForArtist(final Context context, final long id) {
        try {
            final String[] projection = new String[]{
                    BaseColumns._ID
            };
            final String selection = AudioColumns.ARTIST_ID + "=" + id + " AND "
                    + AudioColumns.IS_MUSIC + "=1";
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                    AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK);
            if (cursor != null) {
                final long[] mList = getSongListForCursor(cursor);
                cursor.close();
                if (mList == null || mList.length == 0) {
                    return sEmptyList;
                }
                return mList;
            }
        } catch (Throwable t) {
            return sEmptyList;
        }
        return sEmptyList;
    }

    public static long getAlbumIdForSong(final Context context, final long songId) {
        long albumId = -1;
        final String[] projection = new String[]{
                AudioColumns.ALBUM_ID
        };
        final String selection = AudioColumns._ID + "=" + songId + " AND " + AudioColumns.IS_MUSIC + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null,
                null
        );
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                albumId = cursor.getLong(0);
            } catch (CursorIndexOutOfBoundsException oob) {
                return -1;
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

        }
        return albumId;
    }

    public static String getAlbumName(final Context context, final long id) {
        String albumName = null;
        final String[] projection = new String[]{
                AudioColumns.ALBUM
        };
        final String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null,
                null
        );
        if (cursor != null) {
            cursor.moveToFirst();
            albumName = cursor.getString(0);
            cursor.close();
        }
        return albumName;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the album.
     * @return The song list for an album.
     */
    public static long[] getSongListForAlbum(final Context context, final long id) {
        final String[] projection = new String[]{
                BaseColumns._ID
        };
        final String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC
                + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            final long[] mList = getSongListForCursor(cursor);
            cursor.close();
            return mList;
        }
        return sEmptyList;
    }

    /**
     * Plays songs by an artist.
     *
     * @param context  The {@link Context} to use.
     * @param artistId The artist Id.
     * @param position Specify where to start.
     */
    public static void playArtist(final Context context, final long artistId, int position) {
        final long[] artistList = getSongListForArtist(context, artistId);
        if (artistList != null) {
            playAll(artistList, position, MusicUtils.isShuffleEnabled());
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the genre.
     * @return The song list for an genre.
     */
    public static long[] getSongListForGenre(final Context context, final long id) {
        final String[] projection = new String[]{
                BaseColumns._ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + "!=''");
        final Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        Cursor cursor = context.getContentResolver().query(uri, projection, selection.toString(),
                null, null);
        if (cursor != null) {
            final long[] mList = getSongListForCursor(cursor);
            cursor.close();
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param uri The source of the file
     */
    public static void playFile(final Uri uri) {
        // TODO: Check for PHONE_STATE Permissions here.

        if (uri == null || musicPlaybackService == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-file descriptor code path.
        String filename;
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        try {
            musicPlaybackService.stop();
            musicPlaybackService.openFile(filename);
            musicPlaybackService.play();
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @param list         The list of songs to play.
     * @param position     Specify where to start.
     * @param forceShuffle True to force a shuffle, false otherwise.
     */
    public static void playAll(final long[] list, int position,
                               final boolean forceShuffle) {
        // TODO: Check for PHONE_STATE Permissions here.

        if (list == null || list.length == 0 || musicPlaybackService == null) {
            return;
        }

        try {
            musicPlaybackService.enableShuffle(forceShuffle);
            final long currentId = musicPlaybackService.getAudioId();
            final int currentQueuePosition = getQueuePosition();
            if (continuedPlayingCurrentQueue(list, position, currentId, currentQueuePosition)) {
                return;
            }
            if (position < 0) {
                position = 0;
            }

            musicPlaybackService.open(list, position);
            musicPlaybackService.play();

        } catch (final RemoteException ignored) {
        } catch (NullPointerException e) {
            // we are getting this error because musicPlaybackService is
            // a global static mutable variable, we can't do anything
            // until a full refactor in player
            LOG.warn("Review code logic", e);
        }
    }

    private static boolean continuedPlayingCurrentQueue(long[] list, int position, long currentId, int currentQueuePosition) {
        if (position != -1 && currentQueuePosition == position && currentId == list[position]) {
            final long[] playlist = getQueue();
            if (Arrays.equals(list, playlist)) {
                try {
                    musicPlaybackService.play();
                } catch (Throwable ignored) {
                    ignored.printStackTrace();

                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @param list The list to enqueue.
     */
    public static void playNext(final long[] list) {
        if (musicPlaybackService == null || list == null) {
            return;
        }
        try {
            musicPlaybackService.enqueue(list, MusicPlaybackService.NEXT);
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @param context The {@link Context} to use.
     */
    public static void shuffleAll(final Context context) {
        // TODO: Check for PHONE_STATE Permissions here.
        Cursor cursor = new SongLoader(context).makeCursor(context);
        final long[] mTrackList = getSongListForCursor(cursor);
        final int position = 0;
        if (mTrackList.length == 0 || musicPlaybackService == null) {
            return;
        }
        try {
            musicPlaybackService.enableShuffle(true);
            final long mCurrentId = musicPlaybackService.getAudioId();
            final int mCurrentQueuePosition = getQueuePosition();

            if (continuedPlayingCurrentQueue(mTrackList, position, mCurrentId, mCurrentQueuePosition)) {
                return;
            }
            musicPlaybackService.open(mTrackList, -1);
            musicPlaybackService.play();
            cursor.close();
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the playlist.
     * @return The ID for a playlist.
     */
    public static long getIdForPlaylist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{
                        BaseColumns._ID
                }, PlaylistsColumns.NAME + "=?", new String[]{
                        name
                }, PlaylistsColumns.NAME);
        return getFirstId(cursor, -1);
    }

    /**
     * Returns the Id for an artist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the artist.
     * @return The ID for an artist.
     */
    public static long getIdForArtist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{
                        BaseColumns._ID
                }, ArtistColumns.ARTIST + "=?", new String[]{
                        name
                }, ArtistColumns.ARTIST);
        return getFirstId(cursor, -1);
    }

    /**
     * Returns the ID for an album.
     *
     * @param context    The {@link Context} to use.
     * @param albumName  The name of the album.
     * @param artistName The name of the artist
     * @return The ID for an album.
     */
    public static long getIdForAlbum(final Context context, final String albumName,
                                     final String artistName) {
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                            BaseColumns._ID
                    }, AlbumColumns.ALBUM + "=? AND " + AlbumColumns.ARTIST + "=?", new String[]{
                            albumName, artistName
                    }, AlbumColumns.ALBUM);
        } catch (Throwable t) {
            return -1;
        }

        int id = -1;
        id = getFirstId(cursor, id);
        return id;
    }

    private static int getFirstId(Cursor cursor, int id) {
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    /**
     * Plays songs from an album.
     *
     * @param context  The {@link Context} to use.
     * @param albumId  The album Id.
     * @param position Specify where to start.
     */
    public static void playAlbum(final Context context, final long albumId, int position) {
        final long[] albumList = getSongListForAlbum(context, albumId);
        if (albumList != null) {
            playAll(albumList, position, MusicUtils.isShuffleEnabled());
        }
    }

    /*  */
    public static void makeInsertItems(final long[] ids, final int offset, int len, final int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }

        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    public static List<Playlist> getPlaylists(final Context context) {
        final List<Playlist> result = new ArrayList<>();
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                BaseColumns._ID,
                MediaStore.Audio.PlaylistsColumns.NAME
        };

        try {
            final Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    projection, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(new Playlist(cursor.getLong(0), cursor.getString(1)));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Throwable e) {
            LOG.error("Could not fetch playlists", e);
        }

        return result;
    }

    /**
     * @param context The {@link Context} to use.
     * @param name    The name of the new playlist.
     * @return A new playlist ID.
     */
    public static long createPlaylist(final Context context, final String name) {
        long result = -1;
        if (context != null && name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[]{
                    PlaylistsColumns.NAME
            };
            final String selection = PlaylistsColumns.NAME + " = ?";
            Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    projection, selection, new String[]{name}, null);
            if (cursor != null && cursor.getCount() <= 0) {
                final ContentValues values = new ContentValues(1);
                values.put(PlaylistsColumns.NAME, name);
                final Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values);

                if (uri != null) {
                    result = Long.parseLong(uri.getLastPathSegment());
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * @param context    The {@link Context} to use.
     * @param playlistId The playlist ID.
     */
    public static void clearPlaylist(final Context context, final int playlistId) {
        if (context != null) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
            context.getContentResolver().delete(uri, null, null);
        }
    }

    /**
     * @param context    The {@link Context} to use.
     * @param ids        The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(final Context context, final long[] ids, final long playlistid) {
        if (context == null) {
            LOG.warn("context was null, not adding anything to playlist.");
            return;
        }

        if (ids == null) {
            LOG.warn("song ids given null, not adding anything to playlist.");
            return;
        }

        if (ids == sEmptyList) {
            LOG.warn("song ids was empty, not adding anything to playlist.");
            return;
        }

        long[] currentQueue = getQueue();
        long[] playlist = getSongListForPlaylist(context, playlistid);
        boolean updateQueue = isPlaylistInQueue(playlist, currentQueue);
        final int size = ids.length;
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                "count(*)"
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, projection, null, null, null);
        } catch (Throwable ignored) {
        }

        if (cursor != null) {
            cursor.moveToFirst();
            final int base = cursor.getInt(0);
            cursor.close();
            int numinserted = 0;
            //TODO: Check this portion of code, seems is doing extra work.
            for (int offSet = 0; offSet < size; offSet += 1000) {
                makeInsertItems(ids, offSet, 1000, base);
                numinserted += resolver.bulkInsert(uri, mContentValuesCache);
            }
            refresh();
        } else {
            LOG.warn("Unable to complete addToPlaylist, review the logic");
        }
    }

    private static boolean isPlaylistInQueue(long[] currentQueue, long[] playlist) {
        if (playlist.length == 0 || currentQueue.length == 0 || playlist.length > currentQueue.length) {
            return false;
        }
        for (long p : playlist) {
            boolean foundP = false;
            for (long q : currentQueue) {
                if (p == q) {
                    foundP = true;
                    break;
                }
            }
            if (!foundP) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes a single track from a given playlist
     *
     * @param context          The {@link Context} to use.
     * @param id               The id of the song to remove.
     * @param playlistId       The id of the playlist being removed from.
     * @param showNotification if true shows a notification at the top.
     */
    public static void removeFromPlaylist(final Context context, final long id,
                                          final long playlistId, boolean showNotification) {
        if (context == null) {
            return;
        }

        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, Playlists.Members.AUDIO_ID + " = ? ", new String[]{
                Long.toString(id)
        });
    }

    /**
     * Removes a single track from a given playlist
     *
     * @param context    The {@link Context} to use.
     * @param id         The id of the song to remove.
     * @param playlistId The id of the playlist being removed from.
     */
    public static void removeFromPlaylist(final Context context, final long id,
                                          final long playlistId) {
        removeFromPlaylist(context, id, playlistId, false);
    }

    public static String getFirstStringResult(Cursor cursor, boolean closeCursor) {
        String result = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                result = cursor.getString(0);
            }
            if (closeCursor) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The id of the album.
     * @return The release date for an album.
     */
    public static String getReleaseDateForAlbum(final Context context, final long id) {
        if (context == null || id == -1) {
            return null;
        }
        try {
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
            Cursor cursor = context.getContentResolver().query(uri, new String[]{
                    AlbumColumns.FIRST_YEAR
            }, null, null, null);
            return getFirstStringResult(cursor, true);
        } catch (Throwable e) {
            // ignore this error since it's not critical
            LOG.error("Error getting release date for album", e);
            return null;
        }
    }

    /**
     * @param from The index the item is currently at.
     * @param to   The index the item is moving to.
     */
    public static void moveQueueItem(final int from, final int to) {
        try {
            if (musicPlaybackService != null) {
                musicPlaybackService.moveQueueItem(from, to);
            }
        } catch (final RemoteException ignored) {
        }
    }
    /**
     * @return True if the current song is a favorite, false otherwise.
     */
    public static boolean isFavorite() {
        try {
            if (musicPlaybackService != null) {
                return musicPlaybackService.isFavorite();
            }
        } catch (final RemoteException ignored) {
        }
        return false;
    }

    /**
     * @param context    The {@link Context} to sue
     * @param playlistId The playlist Id
     * @return The track list for a playlist
     */
    public static long[] getSongListForPlaylist(final Context context, final long playlistId) {
        if (context == null) {
            return sEmptyList;
        }

        try {
            final String[] projection = new String[]{
                    MediaStore.Audio.Playlists.Members.AUDIO_ID
            };
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                    projection,
                    null,
                    null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                final long[] list = getSongListForCursor(cursor);
                cursor.close();
                return list;
            }
        } catch (Throwable t) {
            LOG.warn(t.getMessage(), t);
            return sEmptyList;
        }
        return sEmptyList;
    }

    /**
     * @param cursor The {@link Cursor} used to gather the list in our favorites
     *               database
     * @return The song list for the favorite playlist
     */
    public static long[] getSongListForFavoritesCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final long[] list = new long[len];
        cursor.moveToFirst();
        int colidx = -1;
        try {
            colidx = cursor.getColumnIndexOrThrow(FavoriteColumns.ID);
        } catch (final Exception ignored) {
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(colidx);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list from our favorites database
     */
    public static long[] getSongListForFavorites(final Context context) {
        Cursor cursor = FavoritesLoader.makeFavoritesCursor(context);
        if (cursor != null) {
            final long[] list = getSongListForFavoritesCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * Play the songs that have been marked as favorites.
     *
     * @param context The {@link Context} to use
     */
    public static void playFavorites(final Context context) {
        playAll(getSongListForFavorites(context), 0, MusicUtils.isShuffleEnabled());
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list for the last added playlist
     */
    public static long[] getSongListForLastAdded(final Context context) {
        final Cursor cursor = LastAddedLoader.makeLastAddedCursor(context);
        if (cursor != null) {
            final int count = cursor.getCount();
            final long[] list = new long[count];
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            return list;
        }
        return sEmptyList;
    }

    /**
     * Plays the last added songs from the past two weeks.
     *
     * @param context The {@link Context} to use
     */
    public static void playLastAdded(final Context context) {
        playAll(getSongListForLastAdded(context), 0, MusicUtils.isShuffleEnabled());
    }

    /**
     * Creates a sub menu used to add items to a new playlist or an existing
     * one.
     *
     * @param context       The {@link Context} to use.
     * @param groupId       The group Id of the menu.
     * @param subMenu       The {@link SubMenu} to add to.
     * @param showFavorites True if we should show the option to add to the
     *                      Favorites cache.
     */
    public static void makePlaylistMenu(final Context context, final int groupId,
                                        final SubMenu subMenu, final boolean showFavorites) {
        if (context == null) {
            LOG.warn("context was null, not making playlist menu");
            return;
        }

        subMenu.clearHeader();
        Cursor cursor = PlaylistLoader.makePlaylistCursor(context);
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final Intent intent = new Intent();
                String name = cursor.getString(1);
                if (name != null) {
                    intent.putExtra("playlist", getIdForPlaylist(context, name));
                    subMenu.add(groupId, FragmentMenuItems.PLAYLIST_SELECTED, Menu.NONE,
                            name).setIntent(intent).setIcon(R.drawable.contextmenu_icon_add_to_existing_playlist_dark);
                }
                cursor.moveToNext();
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Called when one of the lists should refresh or re-query.
     */
    public static void refresh() {
        try {
            if (musicPlaybackService != null) {
                musicPlaybackService.refresh();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @return The current position time of the track
     */
    public static long position() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.position();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static long duration() {
        if (musicPlaybackService != null) {
            try {
                return musicPlaybackService.duration();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

}
