package zig.zak.media.tor.android.gui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import zig.zak.media.tor.R;
import zig.zak.media.tor.android.core.ConfigurationManager;
import zig.zak.media.tor.android.core.Constants;
import zig.zak.media.tor.android.gui.LocalSearchEngine;
import zig.zak.media.tor.android.gui.activity.MainActivity;
import zig.zak.media.tor.android.gui.adapters.SearchResultListAdapter;
import zig.zak.media.tor.android.gui.adapters.SearchResultListAdapter.FilteredSearchResults;
import zig.zak.media.tor.android.gui.dialogs.HandpickedTorrentDownloadDialogOnFetch;
import zig.zak.media.tor.android.gui.dialogs.NewTransferDialog;
import zig.zak.media.tor.android.gui.services.Engine;
import zig.zak.media.tor.android.gui.tasks.AsyncDownloadSoundcloudFromUrl;
import zig.zak.media.tor.android.gui.tasks.AsyncStartDownload;
import zig.zak.media.tor.android.gui.transfers.TransferManager;
import zig.zak.media.tor.android.gui.util.DirectionDetectorScrollListener;
import zig.zak.media.tor.android.gui.util.ScrollListeners.ComposedOnScrollListener;
import zig.zak.media.tor.android.gui.util.ScrollListeners.FastScrollDisabledWhenIdleOnScrollListener;
import zig.zak.media.tor.android.gui.util.UIUtils;
import zig.zak.media.tor.android.gui.views.AbstractDialog.OnDialogClickListener;
import zig.zak.media.tor.android.gui.views.AbstractFragment;
import zig.zak.media.tor.android.gui.views.KeywordFilterDrawerView;
import zig.zak.media.tor.android.gui.views.SearchInputView;
import zig.zak.media.tor.android.gui.views.SearchProgressView;
import zig.zak.media.tor.android.gui.views.SwipeLayout;
import zig.zak.media.tor.android.offers.SearchHeaderBanner;
import zig.zak.media.tor.search.FileSearchResult;
import zig.zak.media.tor.search.HttpSearchResult;
import zig.zak.media.tor.search.KeywordDetector;
import zig.zak.media.tor.search.KeywordFilter;
import zig.zak.media.tor.search.SearchError;
import zig.zak.media.tor.search.SearchListener;
import zig.zak.media.tor.search.SearchResult;
import zig.zak.media.tor.search.soundcloud.SoundCloudSearchResult;
import zig.zak.media.tor.search.torrent.AbstractTorrentSearchResult;
import zig.zak.media.tor.search.torrent.TorrentCrawledSearchResult;
import zig.zak.media.tor.search.torrent.TorrentSearchResult;
import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.Ref;
import zig.zak.media.tor.uxstats.UXAction;
import zig.zak.media.tor.uxstats.UXStats;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static zig.zak.media.tor.android.util.Asyncs.async;

public final class SearchFragment extends AbstractFragment implements MainFragment, OnDialogClickListener, SearchProgressView.CurrentQueryReporter, KeywordFilterDrawerView.KeywordFilterDrawerController, DrawerLayout.DrawerListener {
    private static final Logger LOG = Logger.getLogger(SearchFragment.class);
    private static final String TAG = SearchFragment.class.getSimpleName();
    private SearchResultListAdapter adapter;
    private SearchInputView searchInput;
    private ProgressBar deepSearchProgress;
    private ListView list;
    private FilterToolbarButton filterButton;
    private String currentQuery;
    private final FileTypeCounter fileTypeCounter;
    private final KeywordDetector keywordDetector;
    private DrawerLayout drawerLayout;
    private KeywordFilterDrawerView keywordFilterDrawerView;
    private SearchHeaderBanner searchHeaderBanner;
    private NativeAd nativeAd;

    public SearchFragment() {
        super(R.layout.fragment_search);
        fileTypeCounter = new FileTypeCounter();
        currentQuery = null;
        keywordDetector = new KeywordDetector();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupAdapter();
        setRetainInstance(true);
    }

    private void loadNativeAd() {
        // Instantiate a NativeAd object.
        // NOTE: the placement ID will eventually identify this as your App, you can ignore it for
        // now, while you are testing and replace it later when you have signed up.
        // While you are using this temporary code you will only get test ads and if you release
        // your code like this to the Google Play your users will not receive ads (you will get a no fill error).
        nativeAd = new NativeAd(getActivity(), getString(R.string.id_ad_native));

        NativeAdListener adListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {
                // Native ad finished downloading all assets
                Log.e(TAG, "Native ad finished downloading all assets.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Native ad failed to load
                Log.e(TAG, "Native ad failed to load: " + adError.getErrorMessage());
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
        Log.i(TAG, "inflateAd");
        nativeAd.unregisterView();

        // Add the Ad view into the ad container.
        NativeAdLayout nativeAdLayout = getView().findViewById(R.id.native_ad_container);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
        View adView = inflater.inflate(R.layout.native_ad_layout, nativeAdLayout, false);
        nativeAdLayout.addView(adView);

        // Add the AdOptionsView
        LinearLayout adChoicesContainer = getView().findViewById(R.id.ad_choices_container);
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
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        LinearLayout header = (LinearLayout) inflater.inflate(R.layout.view_search_header, null, false);
        TextView title = header.findViewById(R.id.view_search_header_text_title);
        title.setText(R.string.search);
        ImageButton filterButtonIcon = header.findViewById(R.id.view_search_header_search_filter_button);
        TextView filterCounter = header.findViewById(R.id.view_search_header_search_filter_counter);
        filterButton = new FilterToolbarButton(filterButtonIcon, filterCounter);
        filterButton.updateVisibility();
        return header;
    }


    @Override
    public void onResume() {
        super.onResume();
        // getHeader was conceived for quick update of main fragments headers,
        // mainly in a functional style, but it is ill suited to extract from
        // it a mutable state, like filterButton.
        // As a result, you will get multiple NPE during the normal lifestyle
        // of the fragmentRef, since getHeader is not guaranteed to be called
        // at the right time during a full resume of the fragmentRef.
        // TODO: refactor this
        if (filterButton == null && isAdded() && getActivity() != null) { // best effort
            // this will happen due to the call to onTabReselected on full resume
            // and this is only solving the NPE, the drawback is that it will
            // create a few orphan view objects to be GC'ed soon.
            // it'is a poor solution overall, but the right one requires
            // a big refactor.
            getHeader(getActivity());
        }
        ConfigurationManager CM = ConfigurationManager.instance();
        if (adapter != null && (adapter.getCount() > 0 || adapter.getTotalCount() > 0)) {
            refreshFileTypeCounters(true);
            searchInput.selectTabByMediaType((byte) CM.getLastMediaTypeFilter());
            filterButton.reset(false);
            boolean filtersApplied = !adapter.getKeywordFiltersPipeline().isEmpty();
            if (filtersApplied) {
                updateKeywordDetector(adapter.filter().keywordFiltered);
            } else {
                updateKeywordDetector(adapter.getList());
            }
            filterButton.updateVisibility();
            keywordFilterDrawerView.updateAppliedKeywordFilters(adapter.getKeywordFiltersPipeline());
        }

        if (list != null) {
            list.setOnScrollListener(new FastScrollDisabledWhenIdleOnScrollListener());
        }

        if (list != null && CM.getBoolean(Constants.PREF_KEY_GUI_DISTRACTION_FREE_SEARCH)) {
            list.setOnScrollListener(new ComposedOnScrollListener(new FastScrollDisabledWhenIdleOnScrollListener(), new DirectionDetectorScrollListener(new ScrollDirectionListener(this), Engine.instance().getThreadPool())));
        }
        if (getCurrentQuery() == null) {
            searchInput.setFileTypeCountersVisible(false);
        }
    }

    @Override
    public void onDestroy() {
        LocalSearchEngine.instance().setListener(null);
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
        destroyHeaderBanner();
        super.onDestroy();
    }

    public void destroyHeaderBanner() {
        if (searchHeaderBanner != null) {
            searchHeaderBanner.onDestroy();
        }
    }

    @Override
    public void onShow() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadNativeAd();
    }

    @Override
    protected void initComponents(final View view, Bundle savedInstanceState) {
        searchHeaderBanner = findView(view, R.id.fragment_search_header_banner);
        searchInput = findView(view, R.id.fragment_search_input);
        searchInput.setShowKeyboardOnPaste(true);
        searchInput.setOnSearchListener(new SearchInputOnSearchListener((LinearLayout) view, this));
        deepSearchProgress = findView(view, R.id.fragment_search_deepsearch_progress);
        deepSearchProgress.setVisibility(GONE);
        list = findView(view, R.id.fragment_search_list);

        SwipeLayout swipe = findView(view, R.id.fragment_search_swipe);
        swipe.setOnSwipeListener(new SwipeLayout.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                switchToThe(true);
            }

            @Override
            public void onSwipeRight() {
                switchToThe(false);
            }
        });
        showSearchView(view);
    }

    private void startMagnetDownload(String magnet) {
        UIUtils.showLongMessage(getActivity(), R.string.torrent_url_added);
        TransferManager.instance().downloadTorrent(magnet, new HandpickedTorrentDownloadDialogOnFetch(getActivity()));
    }

    private static String extractYTId(String ytUrl) {
        String vId = null;
        //noinspection RegExpRedundantEscape
        Pattern pattern = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vId = matcher.group(1);
        }
        return vId;
    }

    private void setupAdapter() {
        if (adapter == null) {
            adapter = new SearchResultListAdapter(getActivity()) {
                @Override
                protected void searchResultClicked(View v) {
                    SearchResult sr = (SearchResult) v.getTag();
                    if (!(sr instanceof SoundCloudSearchResult)) {
                        startTransfer(sr, getString(R.string.download_added_to_queue));
                    } else {
                        getPreviewClickListener().onClick(v);
                    }
                }
            };
            LocalSearchEngine.instance().setListener(new LocalSearchEngineListener(this));
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
                showSearchView(getView());
                refreshFileTypeCounters(true);
                activity.findViewById(R.id.pb).setVisibility(GONE);
                setNativeAdVisibility(GONE);
            });
        }
    }

    private void setNativeAdVisibility(int visibility) {
        getView().findViewById(R.id.native_ad_container).setVisibility(visibility);
        AdView adView = ((MainActivity) getActivity()).adView;
        if (adView == null) return;
        if (visibility == GONE) visibility = VISIBLE;
        else visibility = GONE;
        adView.setVisibility(visibility);
    }

    private void updateKeywordDetector(final List<? extends SearchResult> results) {
        if (filterButton != null) {
            keywordDetector.setKeywordDetectorListener(filterButton);
        }
        if (results != null) {
            boolean searchFinished = LocalSearchEngine.instance().isSearchFinished();
            if (!searchFinished || (keywordDetector.totalHistogramKeys() == 0 && results.size() > 0)) {
                updateKeywordDetectorWithSearchResults(this, results);
            }
        }
    }

    @Override
    public void onDrawerSlide(View view, float v) {
        if ((!isVisible() || currentQuery == null) && view == keywordFilterDrawerView) {
            drawerLayout.closeDrawer(view);
        }
    }

    @Override
    public void onDrawerOpened(View view) {
    }

    @Override
    public void onDrawerClosed(View view) {
        if (view == keywordFilterDrawerView) {
            searchInput.selectTabByMediaType((byte) adapter.getFileType());
        }
        filterButton.updateVisibility();
    }

    @Override
    public void onDrawerStateChanged(int i) {
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
        refreshFileTypeCounters(adapter != null && adapter.getList() != null && adapter.getList().size() > 0);
    }

    private void refreshFileTypeCounters(boolean fileTypeCountersVisible) {
        searchInput.setFileTypeCountersVisible(fileTypeCountersVisible);
        boolean keywordFiltersApplied = adapter.getKeywordFiltersPipeline().size() > 0;
        FilteredSearchResults fsr = fileTypeCounter.fsr;
        int applications = keywordFiltersApplied ? fsr.numFilteredApplications : fsr.numApplications;
        int audios = keywordFiltersApplied ? fsr.numFilteredAudio : fsr.numAudio;
        int documents = keywordFiltersApplied ? fsr.numFilteredDocuments : fsr.numDocuments;
        int pictures = keywordFiltersApplied ? fsr.numFilteredPictures : fsr.numPictures;
        int torrents = keywordFiltersApplied ? fsr.numFilteredTorrents : fsr.numTorrents;
        int videos = keywordFiltersApplied ? fsr.numFilteredVideo : fsr.numVideo;
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_APPLICATIONS, applications);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_AUDIO, audios);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_DOCUMENTS, documents);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_PICTURES, pictures);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_TORRENTS, torrents);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_VIDEOS, videos);
    }

    public void performYTSearch(String query) {
        String ytId = extractYTId(query);
        if (ytId != null) {
            searchInput.setText("");
            searchInput.selectTabByMediaType(Constants.FILE_TYPE_VIDEOS);
            performSearch(ytId, Constants.FILE_TYPE_VIDEOS);
            searchInput.setText("youtube:" + ytId);
        }
    }

    private void performSearch(String query, int mediaTypeId) {
        adapter.clear();
        adapter.setFileType(mediaTypeId);
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        resetKeywordDetector();
        currentQuery = query;
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
        LocalSearchEngine.instance().performSearch(query);
        showSearchView(getView());
        UXStats.instance().log(UXAction.SEARCH_STARTED_ENTER_KEY);
    }

    private void cancelSearch() {
        adapter.clear();
        searchInput.setFileTypeCountersVisible(false);
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        resetKeywordDetector();
        currentQuery = null;
        LocalSearchEngine.instance().cancelSearch();
        showSearchView(getView());
        filterButton.reset(true); // hide=true
        keywordDetector.shutdownHistogramUpdateRequestDispatcher();
    }

    private void showSearchView(View view) {
        boolean searchFinished = LocalSearchEngine.instance().isSearchFinished();

        boolean adapterHasResults = adapter != null && adapter.getCount() > 0;
        if (adapterHasResults) {
            switchView(view, R.id.fragment_search_list);
            deepSearchProgress.setVisibility(searchFinished ? GONE : View.VISIBLE);
            filterButton.updateVisibility();
        } else {
            deepSearchProgress.setVisibility(GONE);
        }
    }

    private void switchView(View v, int id) {
        if (v != null) {
            FrameLayout frameLayout = findView(v, R.id.fragment_search_framelayout);
            int childCount = frameLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = frameLayout.getChildAt(i);
                childAt.setVisibility((childAt.getId() == id) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(NewTransferDialog.TAG) && which == Dialog.BUTTON_POSITIVE) {
            if (Ref.alive(NewTransferDialog.srRef)) {
                startDownload(this.getActivity(), NewTransferDialog.srRef.get(), getString(R.string.download_added_to_queue));
                LocalSearchEngine.instance().markOpened(NewTransferDialog.srRef.get(), adapter);
            }
        }
    }

    private void startTransfer(final SearchResult sr, final String toastMessage) {
        Engine.instance().hapticFeedback();
        if (!(sr instanceof AbstractTorrentSearchResult) && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG)) {
            if (sr instanceof FileSearchResult) {
                try {
                    NewTransferDialog dlg = NewTransferDialog.newInstance((FileSearchResult) sr, false);
                    dlg.show(getFragmentManager());
                } catch (IllegalStateException e) {
                    // android.app.FragmentManagerImpl.checkStateLoss:1323 -> java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
                    // just start the download then if the dialog crapped out.
                    onDialogClick(NewTransferDialog.TAG, Dialog.BUTTON_POSITIVE);
                }
            }
        } else {
            if (isVisible()) {
                startDownload(getActivity(), sr, toastMessage);
            }
        }
        uxLogAction(sr);
    }

    public static void startDownload(Context ctx, SearchResult sr, String message) {
        if (sr instanceof AbstractTorrentSearchResult) {
            UIUtils.showShortMessage(ctx, R.string.fetching_torrent_ellipsis);
        }
        new AsyncStartDownload(ctx, sr, message);
    }

    private void uxLogAction(SearchResult sr) {
        UXStats.instance().log(UXAction.SEARCH_RESULT_CLICKED);
        if (sr instanceof HttpSearchResult) {
            UXStats.instance().log(UXAction.DOWNLOAD_CLOUD_FILE);
        } else if (sr instanceof TorrentSearchResult) {
            if (sr instanceof TorrentCrawledSearchResult) {
                UXStats.instance().log(UXAction.DOWNLOAD_PARTIAL_TORRENT_FILE);
            } else {
                UXStats.instance().log(UXAction.DOWNLOAD_FULL_TORRENT_FILE);
            }
        }
    }

    @Override
    public String getCurrentQuery() {
        return currentQuery;
    }

    private void switchToThe(boolean right) {
        searchInput.switchToThe(right);
    }

    public void connectDrawerLayoutFilterView(DrawerLayout drawerLayout, View filterView) {
        this.drawerLayout = drawerLayout;
        drawerLayout.removeDrawerListener(this);
        drawerLayout.addDrawerListener(this);
        keywordFilterDrawerView = (KeywordFilterDrawerView) filterView;
        keywordFilterDrawerView.setKeywordFilterDrawerController(this);
    }

    @Override
    public void closeKeywordFilterDrawer() {
        if (keywordFilterDrawerView != null) {
            drawerLayout.closeDrawer(keywordFilterDrawerView);
        }
    }

    @Override
    public void openKeywordFilterDrawer() {
        if (drawerLayout == null || keywordFilterDrawerView == null) {
            return;
        }
        drawerLayout.openDrawer(keywordFilterDrawerView);
        keywordDetector.requestHistogramsUpdateAsync(null);
    }

    private void resetKeywordDetector() {
        keywordDetector.reset();
        keywordFilterDrawerView.reset();
    }

    private static class LocalSearchEngineListener implements SearchListener {

        private final WeakReference<SearchFragment> searchFragmentRef;

        LocalSearchEngineListener(SearchFragment searchFragment) {
            searchFragmentRef = Ref.weak(searchFragment);
        }

        @Override
        public void onResults(long token, final List<? extends SearchResult> results) {

            if (Ref.alive(searchFragmentRef)) {
                //noinspection unchecked
                searchFragmentRef.get().onSearchResults((List<SearchResult>) results);
            }
        }

        @Override
        public void onError(long token, SearchError error) {
            LOG.error("Some error in search stream: " + error);
        }

        @Override
        public void onStopped(long token) {
            if (Ref.alive(searchFragmentRef)) {
                SearchFragment searchFragment = searchFragmentRef.get();
                if (searchFragment.isAdded()) {
                    searchFragment.getActivity().runOnUiThread(() -> {
                        if (Ref.alive(searchFragmentRef)) {
                            SearchFragment searchFragment1 = searchFragmentRef.get();
                            searchFragment1.deepSearchProgress.setVisibility(GONE);
                        }
                    });
                }
            }
        }
    }

    private final class SearchInputOnSearchListener implements SearchInputView.OnSearchListener {
        private final WeakReference<LinearLayout> rootViewRef;
        private final WeakReference<SearchFragment> fragmentRef;

        SearchInputOnSearchListener(LinearLayout rootView, SearchFragment fragment) {
            this.rootViewRef = Ref.weak(rootView);
            this.fragmentRef = Ref.weak(fragment);
        }

        public void onSearch(View v, String query, int mediaTypeId) {
            getActivity().findViewById(R.id.pb).setVisibility(View.VISIBLE);
            if (!Ref.alive(fragmentRef) || !Ref.alive(rootViewRef)) {
                return;
            }
            SearchFragment fragment = fragmentRef.get();
            fragment.resetKeywordDetector();
            fragment.searchInput.selectTabByMediaType((byte) mediaTypeId);
            if (query.contains("://m.soundcloud.com/") || query.contains("://soundcloud.com/")) {
                fragment.cancelSearch();
                new AsyncDownloadSoundcloudFromUrl(fragment.getActivity(), query);
                fragment.searchInput.setText("");
            } else if (query.contains("youtube.com/")) {
                fragment.performYTSearch(query);
            } else if (query.startsWith("magnet:?xt=urn:btih:")) {
                fragment.startMagnetDownload(query);
                fragment.currentQuery = null;
                fragment.searchInput.setText("");
            } else {
                fragment.performSearch(query, mediaTypeId);
            }
        }

        public void onMediaTypeSelected(View view, int mediaTypeId) {
            if (!Ref.alive(fragmentRef)) {
                return;
            }
            SearchFragment fragment = fragmentRef.get();
            if (fragment.adapter.getFileType() != mediaTypeId) {
                ConfigurationManager.instance().setLastMediaTypeFilter(mediaTypeId);
                fragment.adapter.setFileType(mediaTypeId);
            }
            fragment.showSearchView(rootViewRef.get());
        }

        public void onClear(View v) {
            if (!Ref.alive(fragmentRef)) {
                return;
            }
            SearchFragment fragment = fragmentRef.get();
            fragment.cancelSearch();
            setNativeAdVisibility(VISIBLE);
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

    private class FilterToolbarButton implements KeywordDetector.KeywordDetectorListener, KeywordFilterDrawerView.KeywordFiltersPipelineListener {

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

        @Override
        public void notifyHistogramsUpdate(final Map<KeywordDetector.Feature, List<Map.Entry<String, Integer>>> filteredHistograms) {
            // TODO: review this, this is a workaround to a not clear framework logic problem
            long td = System.currentTimeMillis() - filterButton.lastUIUpdate;
            if (td <= 300) {
                // don't bother to enqueue the task
                return;
            }

            async(filterButton, SearchFragment::possiblyWaitInBackgroundToUpdateUI, keywordFilterDrawerView, filteredHistograms, SearchFragment::updateUIWithFilteredHistogramsPerFeature);
        }

        @Override
        public void onKeywordDetectorFinished() {
            if (isAdded()) {
                getActivity().runOnUiThread(() -> {
                    keywordFilterDrawerView.hideIndeterminateProgressViews();
                    keywordFilterDrawerView.requestLayout();
                });
            }
        }

        public void reset(boolean hide) { //might do, parameter to not hide drawer
            setVisible(!hide);
            keywordDetector.reset();
            closeKeywordFilterDrawer();
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
            keywordFilterDrawerView.showIndeterminateProgressViews();
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
                openKeywordFilterDrawerView();
                UXStats.instance().log(UXAction.SEARCH_FILTER_BUTTON_CLICK);
            });
        }

        private void openKeywordFilterDrawerView() {
            keywordFilterDrawerView.setKeywordFiltersPipelineListener(this);
            openKeywordFilterDrawer();
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
