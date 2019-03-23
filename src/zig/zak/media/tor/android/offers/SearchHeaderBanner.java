package zig.zak.media.tor.android.offers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

import zig.zak.media.tor.android.core.ConfigurationManager;
import zig.zak.media.tor.android.core.Constants;
import zig.zak.media.tor.android.gui.fragments.SearchFragment;
import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.Ref;

public final class SearchHeaderBanner extends LinearLayout {

    private static final Logger LOG = Logger.getLogger(SearchHeaderBanner.class);

    public enum BannerType {
        MOPUB, FALLBACK, ALL
    }

    private WeakReference<SearchFragment> searchFragmentWeakReference;
    private LinearLayout bannerHeaderLayout;

    private HeaderBannerListener moPubBannerListener;

    public SearchHeaderBanner(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
        }

        public void onDestroy() {
            //LOG.info("HeaderBannerListener.onDestroy()");
            if (!Ref.alive(searchHeaderBannerRef)) {
                LOG.warn("HeaderBannerListener.onDestroy(): check your logic. Could not correctly destroy moPubView, banner reference lost");
                return;
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
