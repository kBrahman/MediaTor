package z.zer.tor.media.android.gui.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import z.zer.tor.media.BuildConfig;
import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.LocalSearchEngine;
import z.zer.tor.media.android.gui.adapters.SearchResultListAdapter;
import z.zer.tor.media.android.gui.adapters.SearchResultListAdapter.FilteredSearchResults;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.android.gui.util.DirectionDetectorScrollListener;
import z.zer.tor.media.android.gui.util.ScrollListeners.ComposedOnScrollListener;
import z.zer.tor.media.android.gui.util.ScrollListeners.FastScrollDisabledWhenIdleOnScrollListener;
import z.zer.tor.media.android.gui.views.AbstractFragment;
import z.zer.tor.media.android.gui.views.KeywordFilterDrawerView;
import z.zer.tor.media.android.gui.views.SearchInputView;
import z.zer.tor.media.search.KeywordDetector;
import z.zer.tor.media.search.KeywordFilter;
import z.zer.tor.media.search.SearchError;
import z.zer.tor.media.search.SearchListener;
import z.zer.tor.media.search.SearchResult;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer;
import z.zer.tor.media.util.Ref;
import z.zer.tor.media.uxstats.UXAction;
import z.zer.tor.media.uxstats.UXStats;

public final class SearchFragment extends AbstractFragment implements SearchListener {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String MEDIA_PLAY_PREFS = "media_play_prefs";
    private static final String S_C_KEY = "s_c_key";
    public SearchResultListAdapter adapter;
    private SearchInputView searchInput;
    private ListView list;
    private String currentQuery;
    private final FileTypeCounter fileTypeCounter;
    private final KeywordDetector keywordDetector;
    private NativeAd nativeAd;

    public SearchFragment() {
        super(R.layout.fragment_search);
        fileTypeCounter = new FileTypeCounter();
        currentQuery = null;
        keywordDetector = new KeywordDetector();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SoundCloudSearchPerformer.SOUND_CLOUD_CLIENT_ID = getActivity()
                .getSharedPreferences(MEDIA_PLAY_PREFS, Context.MODE_PRIVATE).getString(S_C_KEY, BuildConfig.S_C_KEY);
    }

    private void loadFB() {
        nativeAd = new NativeAd(getActivity(), getString(R.string.id_ad_native_fb));
        NativeAdListener adListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {
                // Native ad finished downloading all assets
                Log.e(TAG, "Native ad finished downloading all assets.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Native ad failed to load
                System.out.println(TAG + "Native ad failed to load: " + adError.getErrorMessage() + "test");
            }


            @Override
            public void onAdLoaded(Ad ad) {
                if (nativeAd == null || nativeAd != ad) {
                    return;
                }
                // Inflate Native Ad into Container
                inflateAd(nativeAd);
            }


            @Override
            public void onAdClicked(Ad ad) {
                // Native ad clicked
                Log.d(TAG, "Native ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Native ad impression
                Log.d(TAG, "Native ad impression logged!");
            }
        };
        NativeAdBase.NativeLoadAdConfig loadAdConfig = nativeAd.buildLoadAdConfig().withAdListener(adListener).build();
        nativeAd.loadAd(loadAdConfig);
    }

    private void inflateAd(NativeAd nativeAd) {
        final View view = getView();
        if (view == null) return;
        nativeAd.unregisterView();
        NativeAdLayout nativeAdLayout = view.findViewById(R.id.native_ad_container);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View adView = inflater.inflate(R.layout.native_ad_layout, nativeAdLayout, false);
        nativeAdLayout.addView(adView);
        LinearLayout adChoicesContainer = view.findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(getActivity(), nativeAd, nativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);

        // Create native UI using the ad metadata.
        TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
        MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
        TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
        TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
        TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
        Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

        // Set the Text.
        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
        nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
        nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
        sponsoredLabel.setText(nativeAd.getSponsoredTranslation());

        // Register the Title and CTA button to listen for clicks.
        nativeAd.registerViewForInteraction(adView, nativeAdMedia);
    }


    @Override
    public void onResume() {
        super.onResume();

        ConfigurationManager CM = ConfigurationManager.instance();
        if (adapter != null && (adapter.getCount() > 0 || adapter.getTotalCount() > 0)) {
            boolean filtersApplied = !adapter.getKeywordFiltersPipeline().isEmpty();
            if (filtersApplied) {
                updateKeywordDetector(adapter.filter().keywordFiltered);
            } else {
                updateKeywordDetector(adapter.getList());
            }
        }

        if (list != null) {
            list.setOnScrollListener(new FastScrollDisabledWhenIdleOnScrollListener());
        }

        if (list != null && CM.getBoolean(Constants.PREF_KEY_GUI_DISTRACTION_FREE_SEARCH)) {
            list.setOnScrollListener(new ComposedOnScrollListener(new FastScrollDisabledWhenIdleOnScrollListener(), new DirectionDetectorScrollListener(new ScrollDirectionListener(this), Engine.instance().getThreadPool())));
        }
    }

    @Override
    public void onDestroy() {
        LocalSearchEngine.instance().setListener(null);
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
        super.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupAdapter();
        loadFB();
    }

    @Override
    protected void initComponents(final View view, Bundle savedInstanceState) {
        searchInput = findView(view, R.id.fragment_search_input);
        searchInput.setShowKeyboardOnPaste(true);
        searchInput.setOnSearchListener(new SearchInputOnSearchListener((ConstraintLayout) view, this));
        list = findView(view, R.id.fragment_search_list);
    }

    private void setupAdapter() {
        if (adapter == null) {
            adapter = new SearchResultListAdapter(getActivity()) {
                @Override
                protected void searchResultClicked(View v) {
                    getPreviewClickListener().onClick(v);
                }
            };
        }
        list.setAdapter(adapter);
    }

    private void onSearchResults(final List<SearchResult> results) {
        FilteredSearchResults fsr = adapter.filter(results);
        final List<SearchResult> mediaTypeFiltered = fsr.filtered;
        final List<SearchResult> keywordFiltered = fsr.keywordFiltered;
        fileTypeCounter.add(fsr);
        // if it's a fresh search, make sure to clear keyword detector
        if (adapter.getCount() == 0 && adapter.getKeywordFiltersPipeline().size() == 0) {
            resetKeywordDetector();
        }
        if (adapter.getKeywordFiltersPipeline().isEmpty()) {
            updateKeywordDetector(results);
        } else {
            updateKeywordDetector(keywordFiltered);
        }
        if (isAdded()) {
            Activity activity = getActivity();
            activity.runOnUiThread(() -> {
                adapter.addResults(keywordFiltered, mediaTypeFiltered);
                View view = getView();
                activity.findViewById(R.id.pb).setVisibility(GONE);
                setNativeAdVisibility(view.findViewById(R.id.native_ad_container), GONE);

            });
        }
    }

    private void setNativeAdVisibility(View nativeAd, int visibility) {
        nativeAd.setVisibility(visibility);
    }

    private void updateKeywordDetector(final List<? extends SearchResult> results) {
        if (results != null) {
            boolean searchFinished = LocalSearchEngine.instance().isSearchFinished();
            if (!searchFinished || (keywordDetector.totalHistogramKeys() == 0 && results.size() > 0)) {
                updateKeywordDetectorWithSearchResults(this, results);
            }
        }
    }


    /**
     * When submitting an anonymous Runnable class to the threadpool, the anonymous class's outer object reference (this)
     * reference will not be our SearchFragment, it will be this KeywordDetectorFeeder static class.
     * <p>
     * If this result adding routine ever takes too long there won't be any references to the Fragment
     * thus we avoid any possibility of a Context leak while rotating the screen or going home and coming back.
     * <p>
     * The most this loop can take is about 1 second (maybe 1.5s on slow cpus) when the first big batch of results arrives,
     * otherwise it processes about 20-50 results at the time in up to 80ms. There's a chance the user will rotate
     * the screen by mistake when a search is submitted, otherwise I would've put this code directly on the main
     * thread, but some frames might be skipped, not a good experience whe you hit 'Search'
     */
    private static void updateKeywordDetectorWithSearchResults(SearchFragment fragment, final List<? extends SearchResult> results) {
        final WeakReference<SearchFragment> fragmentRef = Ref.weak(fragment);
        final ArrayList<SearchResult> resultsCopy = new ArrayList<>(results);
        Engine.instance().getThreadPool().execute(() -> {
            if (!Ref.alive(fragmentRef)) {
                return;
            }
            SearchFragment fragment1 = fragmentRef.get();
            if (fragment1 == null) {
                return; // everything is possible
            }
            fragment1.keywordDetector.feedSearchResults(resultsCopy);
            fragment1.keywordDetector.requestHistogramsUpdateAsync(null);
        });
    }

    @Override
    public void onResults(long token, List<SearchResult> results) {
        Log.i(TAG, "on result");
        if (results.isEmpty()) getActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), R.string.empty_search, Toast.LENGTH_LONG).show();
            getActivity().findViewById(R.id.pb).setVisibility(GONE);
        });
        else onSearchResults(results);
    }

    @Override
    public void onError(long token, SearchError error) {
        Log.i(TAG, "getting new id");
        FragmentActivity activity = getActivity();
        activity.runOnUiThread(() -> {
            class JSInterface {
                @JavascriptInterface
                @SuppressWarnings("unused")
                public void processHTML(String html) {
                    Log.i(TAG, "in processHTML. thread=>" + Thread.currentThread().getName());
                    System.out.println("html=>" + html);
                    String id = html.split("=")[1].split("&")[0];
                    Log.i(TAG, "new id=>" + id);
                    SoundCloudSearchPerformer.SOUND_CLOUD_CLIENT_ID = id;
                    activity.getSharedPreferences(MEDIA_PLAY_PREFS, Context.MODE_PRIVATE).edit()
                            .putString(S_C_KEY, id).apply();
                    activity.runOnUiThread(() -> performSearch(currentQuery, ConfigurationManager.instance().getLastMediaTypeFilter()));
                }
            }
            WebView webView = new WebView(requireContext());
            webView.addJavascriptInterface(new JSInterface(), "HTMLOUT");
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    view.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0]" +
                            ".getElementsByTagName('body')[0].getElementsByTagName('div')[0]." +
                            "getElementsByTagName('header')[0].getElementsByTagName('div')[0].children[2]." +
                            "getElementsByTagName('div')[2].getAttribute('data-src'));");
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Safari/537.36");
            webView.loadUrl("https://soundcloud.com");
        });
    }

    @Override
    public void onStopped(long token) {

    }

    private static final class ScrollDirectionListener implements DirectionDetectorScrollListener.ScrollDirectionListener {
        private final WeakReference<SearchFragment> searchFragmentWeakReference;

        ScrollDirectionListener(SearchFragment searchFragment) {
            searchFragmentWeakReference = Ref.weak(searchFragment);
        }

        @Override
        public void onScrollUp() {
            if (Ref.alive(searchFragmentWeakReference)) {
                searchFragmentWeakReference.get().onSearchScrollUp();
            }
        }

        @Override
        public void onScrollDown() {
            if (Ref.alive(searchFragmentWeakReference)) {
                searchFragmentWeakReference.get().onSearchScrollDown();
            }
        }
    }

    private void onSearchScrollDown() {
        hideSearchBox();
    }

    private void onSearchScrollUp() {
        showSearchBox();
    }

    private void showSearchBox() {
        searchInput.showTextInput();
    }

    private void hideSearchBox() {
        searchInput.hideTextInput();
    }

    private void updateFileTypeCounter(FilteredSearchResults filteredSearchResults) {
        if (filteredSearchResults != null) {
            fileTypeCounter.clear();
            fileTypeCounter.add(filteredSearchResults);
        }
    }

    private void performSearch(String query, int mediaTypeId) {
        Log.i(TAG, "performSearch");
        adapter.clear();
        adapter.setFileType(mediaTypeId);
        fileTypeCounter.clear();
        resetKeywordDetector();
        currentQuery = query;
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
        LocalSearchEngine.instance().setListener(this);
        LocalSearchEngine.instance().performSearch(query);
        UXStats.instance().log(UXAction.SEARCH_STARTED_ENTER_KEY);
    }

    private void cancelSearch() {
        adapter.clear();
        fileTypeCounter.clear();
        resetKeywordDetector();
        currentQuery = null;
        LocalSearchEngine.instance().cancelSearch();
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
    }

    private void resetKeywordDetector() {
        keywordDetector.reset();
    }

    private final class SearchInputOnSearchListener implements SearchInputView.OnSearchListener {
        private final WeakReference<ViewGroup> rootViewRef;
        private final WeakReference<SearchFragment> fragmentRef;

        SearchInputOnSearchListener(ViewGroup rootView, SearchFragment fragment) {
            this.rootViewRef = Ref.weak(rootView);
            this.fragmentRef = Ref.weak(fragment);
        }

        public void onSearch(View v, String query, int mediaTypeId) {
            getActivity().findViewById(R.id.pb).setVisibility(View.VISIBLE);
            if (!Ref.alive(fragmentRef) || !Ref.alive(rootViewRef)) {
                Log.i(TAG, "not alive");
                return;
            }
            SearchFragment fragment = fragmentRef.get();
            fragment.resetKeywordDetector();
            fragment.performSearch(query, mediaTypeId);
        }

        public void onClear(View v) {
            if (!Ref.alive(fragmentRef)) {
                return;
            }
            SearchFragment fragment = fragmentRef.get();
            fragment.cancelSearch();
            setNativeAdVisibility(getView().findViewById(R.id.native_ad_container), VISIBLE);
            getActivity().findViewById(R.id.pb).setVisibility(GONE);
        }
    }

    private static final class FileTypeCounter {
        private final FilteredSearchResults fsr = new FilteredSearchResults();

        public void add(FilteredSearchResults fsr) {
            this.fsr.numAudio += fsr.numAudio;
            this.fsr.numApplications += fsr.numApplications;
            this.fsr.numDocuments += fsr.numDocuments;
            this.fsr.numPictures += fsr.numPictures;
            this.fsr.numTorrents += fsr.numTorrents;
            this.fsr.numVideo += fsr.numVideo;
            this.fsr.numFilteredAudio += fsr.numFilteredAudio;
            this.fsr.numFilteredApplications += fsr.numFilteredApplications;
            this.fsr.numFilteredDocuments += fsr.numFilteredDocuments;
            this.fsr.numFilteredPictures += fsr.numFilteredPictures;
            this.fsr.numFilteredTorrents += fsr.numFilteredTorrents;
            this.fsr.numFilteredVideo += fsr.numFilteredVideo;
        }

        public void clear() {
            this.fsr.numAudio = 0;
            this.fsr.numApplications = 0;
            this.fsr.numDocuments = 0;
            this.fsr.numPictures = 0;
            this.fsr.numTorrents = 0;
            this.fsr.numVideo = 0;
            this.fsr.numFilteredAudio = 0;
            this.fsr.numFilteredApplications = 0;
            this.fsr.numFilteredDocuments = 0;
            this.fsr.numFilteredPictures = 0;
            this.fsr.numFilteredTorrents = 0;
            this.fsr.numFilteredVideo = 0;
        }
    }

    private class FilterToolbarButton implements KeywordFilterDrawerView.KeywordFiltersPipelineListener {

        private final ImageButton imageButton;
        private final TextView counterTextView;
        private Animation pulse;
        private boolean filterButtonClickedBefore;
        private long lastUIUpdate = 0;

        FilterToolbarButton(ImageButton imageButton, TextView counterTextView) {
            this.imageButton = imageButton;
            this.counterTextView = counterTextView;
            this.filterButtonClickedBefore = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SEARCH_FILTER_DRAWER_BUTTON_CLICKED);
            if (!filterButtonClickedBefore) {
                this.pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
            }
            initListeners();
        }

        // self determine if it should be hidden or not
        public void updateVisibility() {
            setVisible(currentQuery != null && adapter != null && adapter.getTotalCount() > 0);
        }

        public void reset(boolean hide) { //might do, parameter to not hide drawer
            setVisible(!hide);
            keywordDetector.reset();
        }

        @Override
        public void onPipelineUpdate(List<KeywordFilter> pipeline) {
            // this will make the adapter filter
            FilteredSearchResults filteredSearchResults = adapter.setKeywordFiltersPipeline(pipeline);
            updateFileTypeCounter(filteredSearchResults);
            if (pipeline != null) {
                if (pipeline.isEmpty()) {
                    counterTextView.setText("");
                } else {
                    counterTextView.setText(String.valueOf(pipeline.size()));
                }
            }
            updateVisibility();
            List<SearchResult> results = adapter.getKeywordFiltersPipeline().isEmpty() ? adapter.getList() : filteredSearchResults.keywordFiltered;
            keywordDetector.reset();
            keywordDetector.requestHistogramsUpdateAsync(results);
        }

        @Override
        public void onAddKeywordFilter(KeywordFilter keywordFilter) {
            keywordDetector.clearHistogramUpdateRequestDispatcher();
            FilteredSearchResults filteredSearchResults = adapter.addKeywordFilter(keywordFilter);
            updateFileTypeCounter(filteredSearchResults);
        }

        @Override
        public void onRemoveKeywordFilter(KeywordFilter keywordFilter) {
            keywordDetector.clearHistogramUpdateRequestDispatcher();
            updateFileTypeCounter(adapter.removeKeywordFilter(keywordFilter));
        }

        @Override
        public List<KeywordFilter> getKeywordFiltersPipeline() {
            if (adapter == null) {
                return new ArrayList<>(0);
            }
            return adapter.getKeywordFiltersPipeline();
        }

        private void setVisible(boolean visible) {
            int visibility = visible ? View.VISIBLE : GONE;
            int oldVisibility = imageButton.getVisibility();
            imageButton.setVisibility(visibility);
            if (visible) {
                if (oldVisibility == GONE && !filterButtonClickedBefore) {
                    pulse.reset();
                    imageButton.setAnimation(pulse);
                    pulse.setStartTime(AnimationUtils.currentAnimationTimeMillis() + 1000);
                }
                counterTextView.setVisibility(getKeywordFiltersPipeline().size() > 0 ? View.VISIBLE : GONE);
                counterTextView.setText(String.valueOf(getKeywordFiltersPipeline().size()));
            } else {
                imageButton.clearAnimation();
                counterTextView.setVisibility(GONE);
            }
        }

        private void initListeners() {
            imageButton.setOnClickListener(v -> {
                if (!filterButtonClickedBefore) {
                    filterButtonClickedBefore = true;
                    ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SEARCH_FILTER_DRAWER_BUTTON_CLICKED, true);
                    imageButton.clearAnimation();
                    pulse = null;
                }
                UXStats.instance().log(UXAction.SEARCH_FILTER_BUTTON_CLICK);
            });
        }
    }

    @SuppressWarnings("unused")
    private static void possiblyWaitInBackgroundToUpdateUI(FilterToolbarButton filterToolbarButton, KeywordFilterDrawerView keywordFilterDrawerView, Map<KeywordDetector.Feature, List<Map.Entry<String, Integer>>> filteredHistograms) {
        long timeSinceLastUpdate = System.currentTimeMillis() - filterToolbarButton.lastUIUpdate;
        if (timeSinceLastUpdate < 500) {
            try {
                Thread.sleep(500L - timeSinceLastUpdate);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void updateUIWithFilteredHistogramsPerFeature(FilterToolbarButton filterToolbarButton, KeywordFilterDrawerView keywordFilterDrawerView, Map<KeywordDetector.Feature, List<Map.Entry<String, Integer>>> filteredHistograms) {
        filterToolbarButton.lastUIUpdate = System.currentTimeMillis();
        // should be safe from concurrent modification exception as new list with filtered elements
        for (KeywordDetector.Feature feature : filteredHistograms.keySet()) {
            List<Map.Entry<String, Integer>> filteredHistogram = filteredHistograms.get(feature);
            keywordFilterDrawerView.updateData(feature, filteredHistogram);
        }
        filterToolbarButton.updateVisibility();
        keywordFilterDrawerView.requestLayout();
    }
}
