/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.offers;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.util.Logger;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;

/**
 * @author aldenml
 * @author gubatron
 * @author marcelinkaaa
 */
public final class SearchHeaderBanner extends LinearLayout {

    private static final Logger LOG = Logger.getLogger(SearchHeaderBanner.class);

    public enum BannerType {
        MOPUB, FALLBACK, ALL
    }

    private WeakReference<SearchFragment> searchFragmentWeakReference;
    private LinearLayout bannerHeaderLayout;
    private ImageButton dismissBannerButton;

    private HeaderBannerListener moPubBannerListener;

    public SearchHeaderBanner(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            try {
                inflater.inflate(R.layout.view_search_header_banner, this, true);
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
                t.printStackTrace();
            }
        }
    }

    public void setSearchFragmentReference(SearchFragment searchFragment) {
        searchFragmentWeakReference = Ref.weak(searchFragment);
    }

    public void onDestroy() {
        bannerHeaderLayout = null;
        getMoPubBannerListener().onDestroy(); // calls moPubView.onDestroy() and unregisters its IntentReceiver
    }

    private HeaderBannerListener getMoPubBannerListener() {
        if (moPubBannerListener == null) {
            moPubBannerListener = new HeaderBannerListener(this);
        }
        return moPubBannerListener;
    }

    private SearchFragment getSearchFragment() {
        if (!Ref.alive(searchFragmentWeakReference)) {
            return null;
        }
        return searchFragmentWeakReference.get();
    }

    private String getCurrentQuery() {
        SearchFragment searchFragment = getSearchFragment();
        if (searchFragment == null) {
            return null;
        }
        return searchFragment.getCurrentQuery();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        bannerHeaderLayout = findViewById(R.id.fragment_search_advertisement_header_layout);
        bannerHeaderLayout.setVisibility(View.GONE);
    }

    public void updateComponents() {
        if (getSearchFragment() == null) {
            return;
        }
        Activity activity = (Activity) getContext();
        // check how long getting display metrics twice is, if expensive gotta refactor these methods
        boolean screenTallEnough = UIUtils.getScreenInches(activity) >= 4.33;
        boolean isPortrait = UIUtils.isPortrait(activity);
        boolean isTablet = UIUtils.isTablet(activity.getResources());
        boolean diceRollPassed = UIUtils.diceRollPassesThreshold(ConfigurationManager.instance(), Constants.PREF_KEY_GUI_MOPUB_SEARCH_HEADER_BANNER_THRESHOLD);
        setBannerViewVisibility(BannerType.MOPUB, false);

    }


    /**
     * You are responsible for hiding and showing every banner
     */
    public void setBannerViewVisibility(BannerType bannerType, boolean visible) {
        if (bannerHeaderLayout == null) {
            onFinishInflate();
        }
        // LOG.info("setBannerViewVisibility() -> bannerHeaderLayout@"+bannerHeaderLayout.hashCode());
        int visibility = visible ? View.VISIBLE : View.GONE;
        bannerHeaderLayout.setVisibility(visibility);
        // LOG.info("setBannerViewVisibility() bannerHeaderLayout.visible==" + (bannerHeaderLayout.getVisibility() == View.VISIBLE));
    }

    private static final class HeaderBannerListener {
        private final WeakReference<SearchHeaderBanner> searchHeaderBannerRef;
        private long lastDismissed = 0L;
        private final int dismissIntervalInMs;

        HeaderBannerListener(SearchHeaderBanner searchFragment) {
            searchHeaderBannerRef = Ref.weak(searchFragment);
            dismissIntervalInMs = ConfigurationManager.instance().getInt(Constants.PREF_KEY_GUI_MOPUB_SEARCH_HEADER_BANNER_DISMISS_INTERVAL_IN_MS);
        }

        public boolean tooEarlyToDisplay() {
            return (System.currentTimeMillis() - lastDismissed) < dismissIntervalInMs;
        }

        public void onBannerDismissed(BannerType bannerType) {
            //LOG.info("onBannerDismissed(bannerType=" + bannerType + ")");
            if (bannerType == BannerType.FALLBACK) {
                // only changes when the banner container is fully dismissed
                lastDismissed = System.currentTimeMillis();
            }
            if (!Ref.alive(searchHeaderBannerRef)) {
                return;
            }
            searchHeaderBannerRef.get().setBannerViewVisibility(bannerType, false);
        }

        public void onDestroy() {
            //LOG.info("HeaderBannerListener.onDestroy()");
            if (!Ref.alive(searchHeaderBannerRef)) {
                LOG.warn("HeaderBannerListener.onDestroy(): check your logic. Could not correctly destroy moPubView, banner reference lost");
                return;
            }
            SearchHeaderBanner searchHeaderBanner = searchHeaderBannerRef.get();
            try {
                searchHeaderBanner.setBannerViewVisibility(BannerType.ALL, false);
            } catch (Throwable throwable) {
                LOG.error(throwable.getMessage(), throwable);
            }
        }
    }

    private static final class DismissBannerButtonClickListener {
        private final WeakReference<SearchHeaderBanner> searchHeaderBannerRef;

        DismissBannerButtonClickListener(SearchHeaderBanner searchHeaderBanner) {
            searchHeaderBannerRef = Ref.weak(searchHeaderBanner);
        }
    }

}
