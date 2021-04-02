package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

/**
 * Used to query {MediaStore.Audio.Media.EXTERNAL_CONTENT_URI} and return
 * the Song the user added over the past four of weeks.
 */
public class LastAddedLoader extends SongLoader {

    /**
     * Constructor of <code>LastAddedHandler</code>
     *
     * @param context The {@link Context} to use.
     */
    public LastAddedLoader(final Context context) {
        super(context);
    }

    @Override
    public Cursor makeCursor(Context context) {
        return makeLastAddedCursor(getContext());
    }

    /**
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makeLastAddedCursor(final Context context) {
        final int fourWeeks = 4 * 3600 * 24 * 7;
        final StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + AudioColumns.TITLE + " != ''"); //$NON-NLS-2$
        selection.append(" AND " + MediaStore.Audio.Media.DATE_ADDED + ">"); //$NON-NLS-2$
        selection.append(System.currentTimeMillis() / 1000 - fourWeeks);
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
                }, selection.toString(), null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
    }
}
