package z.zer.tor.media.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;

import z.zer.tor.media.utils.PreferenceUtils;

/**
 * Used to query {@link MediaStore.Audio.Artists.Albums} and return the albums
 * for a particular artist.
 */
public class ArtistAlbumLoader extends AlbumLoader {

    /**
     * The Id of the artist the albums belong to.
     */
    private final Long mArtistID;

    /**
     * Constructor of <code>ArtistAlbumHandler</code>
     *
     * @param context  The {@link Context} to use.
     * @param artistId The Id of the artist the albums belong to.
     */
    public ArtistAlbumLoader(final Context context, final Long artistId) {
        super(context);
        mArtistID = artistId;
    }

    @Override
    public Cursor makeCursor(Context context) {
        return makeArtistAlbumCursor(context, mArtistID);
    }

    /**
     * @param context  The {@link Context} to use.
     * @param artistId The Id of the artist the albums belong to.
     */
    private static Cursor makeArtistAlbumCursor(final Context context, final Long artistId) {
        if (artistId == -1) {
            // fix an error reported in Play console
            return null;
        }

        try {
            return context.getContentResolver().query(
                    MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), new String[]{
                            /* 0 */
                            BaseColumns._ID,
                            /* 1 */
                            AlbumColumns.ALBUM,
                            /* 2 */
                            AlbumColumns.ARTIST,
                            /* 3 */
                            AlbumColumns.NUMBER_OF_SONGS,
                            /* 4 */
                            AlbumColumns.FIRST_YEAR
                    }, null, null, PreferenceUtils.getInstance().getArtistAlbumSortOrder());
        } catch (Throwable e) {
            // ignore any error since it's not critical
            return null;
        }
    }
}
