package com.andrew.apollo.ui.fragments.profile;

import android.content.Loader;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.andrew.apollo.adapters.ProfileSongAdapter;
import com.andrew.apollo.loaders.LastAddedLoader;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.fragments.Fragments;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

import z.zer.tor.media.R;

public final class LastAddedFragment extends ApolloFragment<ProfileSongAdapter, Song> {

    public LastAddedFragment() {
        super(Fragments.LAST_ADDED_PROFILE_FRAGMENT_GROUP_ID, Fragments.LAST_ADDED_PROFILE_FRAGMENT_LOADER_ID);
    }

    protected ProfileSongAdapter createAdapter() {
        return new ProfileSongAdapter(
                getActivity(),
                R.layout.list_item_simple,
                ProfileSongAdapter.DISPLAY_PLAYLIST_SETTING
        );
    }

    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new LastAddedLoader(getActivity());
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        onSongItemClick(position);
    }

    @Override
    protected String getLayoutTypeName() {
        return PreferenceUtils.SIMPLE_LAYOUT;
    }
}
