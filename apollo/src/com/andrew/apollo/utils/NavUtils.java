package com.andrew.apollo.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.provider.MediaStore;

import com.andrew.apollo.Config;
import com.andrew.apollo.ui.activities.SearchActivity;
import com.devspark.appmsg.AppMsg;

import z.zer.tor.media.R;

/**
 * Various navigation helpers.
 */
public final class NavUtils {

    private static final String TAG = NavUtils.class.getSimpleName();

    /**
     * Opens the profile of an artist.
     *
     * @param context    The {@link Activity} to use.
     * @param artistName The name of the artist
     */
    public static void openArtistProfile(final Activity context, final String artistName, final long[] songs) {
        if (artistName == null || artistName.isEmpty()) {
            return;
        }

        // Create a new bundle to transfer the artist info
        final Bundle bundle = new Bundle();
        bundle.putLong(Config.ID, MusicUtils.getIdForArtist(context, artistName));
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
        bundle.putString(Config.ARTIST_NAME, artistName);

        if (songs != null && songs.length > 0) {
            bundle.putLongArray(Config.TRACKS, songs);
        }
    }

    /**
     * Opens the profile of an album.
     *
     * @param context    The {@link Activity} to use.
     * @param albumName  The name of the album
     * @param artistName The name of the album artist
     * @param albumId    The id of the album
     */
    public static void openAlbumProfile(final Activity context,
                                        final String albumName, final String artistName, final long albumId, final long[] songs) {

        // Create a new bundle to transfer the album info
        final Bundle bundle = new Bundle();
        bundle.putString(Config.ALBUM_YEAR, MusicUtils.getReleaseDateForAlbum(context, albumId));
        bundle.putString(Config.ARTIST_NAME, artistName);
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
        bundle.putLong(Config.ID, albumId);
        bundle.putString(Config.NAME, albumName);

        if (songs != null && songs.length > 0) {
            bundle.putLongArray(Config.TRACKS, songs);
        }
    }

}
