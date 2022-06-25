package z.zer.tor.media.utils;


import z.zer.tor.media.android.core.ConfigurationManager;

/**
 * A collection of helpers designed to get and set various preferences across
 * Apollo.
 * <p>
 * These helpers are now a wrapper of more optimized FrostWire ConfigurationManager
 */
public final class PreferenceUtils {

    /* Default start page (Artist page) */
    public static final int DEFAULT_PAGE = 0;

    /* Saves the last page the pager was on in {@link MusicBrowserPhoneFragment} */
    public static final String START_PAGE = "start_page";

    // Sort order for the artist list
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";

    // Sort order for the artist song list
    public static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";

    // Sort order for the artist album list
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";

    // Sort order for the album list
    public static final String ALBUM_SORT_ORDER = "album_sort_order";

    // Sort order for the album song list
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";

    // Sort order for the song list
    public static final String SONG_SORT_ORDER = "song_sort_order";

    // Sets the type of layout to use for the artist list
    public static final String ARTIST_LAYOUT = "artist_layout";

    // Sets the type of layout to use for the album list
    public static final String ALBUM_LAYOUT = "album_layout";

    // Sets the type of layout to use for the recent list
    public static final String RECENT_LAYOUT = "recent_layout";

    public static final String SIMPLE_LAYOUT = "simple";

    private static PreferenceUtils sInstance;
    private final ConfigurationManager cm;

    private PreferenceUtils() {
        cm = ConfigurationManager.instance();
    }

    /**
     * @return A singleton of this class
     */
    public static PreferenceUtils getInstance() {
        if (sInstance == null) {
            sInstance = new PreferenceUtils();
        }
        return sInstance;
    }

    public void setStartPage(final int value) {
        cm.setInt(START_PAGE, value);
    }

    public final String getArtistSortOrder() {
        // This is only to prevent return an invalid field name caused by bug BUGDUMP-21136
        final String defaultSortKey = SortOrder.ArtistSortOrder.ARTIST_A_Z;
        String key = cm.getString(ARTIST_SORT_ORDER, defaultSortKey);
        if (key.equals(SortOrder.ArtistSongSortOrder.SONG_FILENAME)) {
            key = defaultSortKey;
        }
        return key;
    }

    public final String getArtistSongSortOrder() {
        return cm.getString(ARTIST_SONG_SORT_ORDER, SortOrder.ArtistSongSortOrder.SONG_A_Z);
    }
    public final String getArtistAlbumSortOrder() {
        return cm.getString(ARTIST_ALBUM_SORT_ORDER, SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
    }


    public final String getAlbumSortOrder() {
        return cm.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
    }

    public final String getAlbumSongSortOrder() {
        return cm.getString(ALBUM_SONG_SORT_ORDER, SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
    }

    public final String getSongSortOrder() {
        return cm.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
    }

    /**
     * Saves the layout type for a list
     *
     * @param key   Which layout to change
     * @param value The new layout type
     */
    private void setPreference(final String key, final String value) {
        cm.setString(key, value);
    }

    /**
     * Sets the layout type for the artist list
     *
     * @param value The new layout type
     */
    public void setArtistLayout(final String value) {
        setPreference(ARTIST_LAYOUT, value);
    }

    /**
     * Sets the layout type for the album list
     *
     * @param value The new layout type
     */
    public void setAlbumLayout(final String value) {
        setPreference(ALBUM_LAYOUT, value);
    }

    /**
     * Sets the layout type for the recent list
     *
     * @param value The new layout type
     */
    public void setRecentLayout(final String value) {
        setPreference(RECENT_LAYOUT, value);
    }

    /**
     * @param which Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isSimpleLayout(final String which) {
        final String simple = "simple";
        final String defaultValue = "grid";
        return cm.getString(which, defaultValue).equals(simple);
    }

    /**
     * @param which Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isDetailedLayout(final String which) {
        final String detailed = "detailed";
        final String defaultValue = "grid";
        return cm.getString(which, defaultValue).equals(detailed);
    }
}
