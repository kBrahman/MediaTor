package com.andrew.apollo.ui.fragments;

import android.view.View;
import android.widget.AdapterView;

import com.andrew.apollo.adapters.AlbumAdapter;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.ui.fragments.profile.ApolloFragment;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

public abstract class BaseAlbumFragment extends ApolloFragment<AlbumAdapter, Album> {

    public BaseAlbumFragment(int groupId, int loaderId) {
        super(groupId, loaderId);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        mItem = mAdapter.getItem(position);
        NavUtils.openAlbumProfile(getActivity(),
                mItem.mAlbumName,
                mItem.mArtistName,
                mItem.mAlbumId,
                MusicUtils.getSongListForAlbum(getActivity(), mItem.mAlbumId));
    }

    protected boolean isSimpleLayout() {
        return PreferenceUtils.getInstance().isSimpleLayout(getLayoutTypeName());
    }
}
