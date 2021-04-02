package com.andrew.apollo.ui.fragments;

import android.app.Fragment;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.andrew.apollo.adapters.SongAdapter;
import com.andrew.apollo.loaders.LastAddedLoader;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.fragments.profile.ApolloFragment;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

import z.zer.tor.media.R;

/**
 * This class is used to display all of the songs on a user's device.
 */
public final class LastAddedFragment extends ApolloFragment<SongAdapter, Song> {

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public LastAddedFragment() {
        super(Fragments.LAST_ADDED_FRAGMENT_GROUP_ID, Fragments.LAST_ADDED_FRAGMENT_LOADER_ID);
    }

    @Override
    protected SongAdapter createAdapter() {
        return new SongAdapter(getActivity(), R.layout.list_item_simple_image);
    }

    @Override
    protected String getLayoutTypeName() {
        return PreferenceUtils.SIMPLE_LAYOUT;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        onSongItemClick(position);
    }

    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new LastAddedLoader(getActivity());
    }

    @Override
    protected boolean isSimpleLayout() {
        return true;
    }

    @Override
    public void onMetaChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoadFinished(final Loader<List<Song>> loader, final List<Song> data) {
        mDefaultFragmentEmptyString = R.string.empty_last_added;
        super.onLoadFinished(loader, data);
    }
}
