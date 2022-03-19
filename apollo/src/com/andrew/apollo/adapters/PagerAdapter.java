package com.andrew.apollo.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import z.zer.tor.media.R;
import z.zer.tor.media.android.gui.views.FragmentPagerAdapter;

/**
 * A {@link FragmentPagerAdapter} class for swiping between playlists, recent,
 * artists, albums, songs, and genre {@link Fragment}s on phones.<br/>
 */
public class PagerAdapter extends FragmentPagerAdapter {

    private final SparseArray<WeakReference<Fragment>> mFragmentArray = new SparseArray<>();

    private final ArrayList<Holder> mHolderList = new ArrayList<>();

    private final Activity mFragmentActivity;

    /**
     * Constructor of <code>PagerAdapter<code>
     *
     * @param fragmentActivity The {@link Activity} of the
     *                         {@link Fragment}.
     */
    public PagerAdapter(final Activity fragmentActivity) {
        super(fragmentActivity.getFragmentManager());
        mFragmentActivity = fragmentActivity;
    }

    /**
     * Method that adds a new fragment class to the viewer (the fragment is
     * internally instantiate)
     *
     * @param className The full qualified name of fragment class.
     * @param params    The instantiate params.
     */
    public void add(final Class<? extends Fragment> className, final Bundle params) {
        final Holder mHolder = new Holder();
        mHolder.mClassName = className.getName();
        mHolder.mParams = params;

        final int mPosition = mHolderList.size();
        mHolderList.add(mPosition, mHolder);
        notifyDataSetChanged();
    }

    /**
     * Method that returns the {@link Fragment} in the argument
     * position.
     *
     * @param position The position of the fragment to return.
     * @return Fragment The {@link Fragment} in the argument position.
     */
    public Fragment getFragment(final int position) {
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null && mWeakFragment.get() != null) {
            return mWeakFragment.get();
        }
        return getItem(position);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        final Fragment mFragment = (Fragment) super.instantiateItem(container, position);
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
        mFragmentArray.put(position, new WeakReference<>(mFragment));
        return mFragment;
    }

    @Override
    public Fragment getItem(final int position) {
        final Holder mCurrentHolder = mHolderList.get(position);
        return Fragment.instantiate(mFragmentActivity,
                mCurrentHolder.mClassName, mCurrentHolder.mParams);
    }

    @Override
    public void destroyItem(final ViewGroup container, final int position, final Object object) {
        super.destroyItem(container, position, object);
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
    }

    @Override
    public int getCount() {
        return mHolderList.size();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        return mFragmentActivity.getResources().getStringArray(R.array.page_titles)[position]
                .toUpperCase(Locale.getDefault());
    }
    private final static class Holder {
        String mClassName;

        Bundle mParams;
    }
}
