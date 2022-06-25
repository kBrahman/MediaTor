package z.zer.tor.media.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import z.zer.tor.media.model.Song;
import z.zer.tor.media.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import z.zer.tor.media.util.Logger;

/**
 * Used to query MediaStore.Audio.Media.EXTERNAL_CONTENT_URI and return
 * the songs on a user's device.
 */
public class SongLoader extends WrappedAsyncTaskLoader<List<Song>> {
    private static final Logger LOGGER = Logger.getLogger(SongLoader.class);

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public SongLoader(final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        ArrayList<Song> mSongList = new ArrayList<>();
        // Create the Cursor
        Cursor mCursor;
        try {
            mCursor = makeCursor(getContext());
        } catch (Throwable ignored) {
            LOGGER.error("SongLoader.loadInBackground(): " + ignored.getMessage(), ignored);
            return Collections.emptyList();
        }

        if (mCursor == null) {
            //LOGGER.warn("loadInBackground() - cursor == null, returning empty list.");
            return Collections.emptyList();
        }

        // Gather the data
        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                // Copy the song Id
                final long id = mCursor.getLong(0);

                // Copy the song name
                final String songName = mCursor.getString(1);

                // Copy the artist name
                final String artist = mCursor.getString(2);

                // Copy the album name
                final String album = mCursor.getString(3);

                // Copy the duration (Not available for all song Cursors, like on FavoritesLoader's)
                long duration = -1;
                int durationInSecs = -1;
                try {
                    duration = mCursor.getLong(4);
                    durationInSecs = (int) duration / 1000;
                } catch (Throwable ignored) {
                    LOGGER.error("SongLoader.loadInBackground(): " + ignored.getMessage(), ignored);
                }

                // Create a new song
                final Song song = new Song(id, songName, artist, album, durationInSecs);

                // Add everything up
                mSongList.add(song);
            } while (mCursor.moveToNext());
        }
        // Close the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        //LOGGER.info("loadInBackground() done (" + mSongList.size() + " songs)");
        return mSongList;
    }

    public Cursor makeCursor(final Context context) {
        return makeSongCursor(context);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    private static Cursor makeSongCursor(final Context context) {
        final StringBuilder mSelection = new StringBuilder();
        mSelection.append(AudioColumns.IS_MUSIC + "=1");
        mSelection.append(" AND " + AudioColumns.TITLE + " != ''"); //$NON-NLS-2$
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
                }, mSelection.toString(), null,
                PreferenceUtils.getInstance().getSongSortOrder());
    }
}
