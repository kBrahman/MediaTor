package zig.zak.media.tor.android.offers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.Ref;

public final class SearchHeaderBanner extends LinearLayout {

    private static final Logger LOG = Logger.getLogger(SearchHeaderBanner.class);

    private HeaderBannerListener moPubBannerListener;

    public SearchHeaderBanner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onDestroy() {
        getMoPubBannerListener().onDestroy(); // calls moPubView.onDestroy() and unregisters its IntentReceiver
    }

    private HeaderBannerListener getMoPubBannerListener() {
        if (moPubBannerListener == null) {
            moPubBannerListener = new HeaderBannerListener(this);
        }
        return moPubBannerListener;
    }

    private static final class HeaderBannerListener {
        private final WeakReference<SearchHeaderBanner> searchHeaderBannerRef;

        HeaderBannerListener(SearchHeaderBanner searchFragment) {
            searchHeaderBannerRef = Ref.weak(searchFragment);
        }

        public void onDestroy() {
            if (!Ref.alive(searchHeaderBannerRef)) {
                LOG.warn("HeaderBannerListener.onDestroy(): check your logic. Could not correctly destroy moPubView, banner reference lost");
            }
        }
    }

}
