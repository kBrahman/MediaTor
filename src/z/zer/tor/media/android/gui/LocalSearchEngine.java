package z.zer.tor.media.android.gui;


import android.text.Html;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import z.zer.tor.media.android.gui.views.AbstractListAdapter;
import z.zer.tor.media.search.CrawlPagedWebSearchPerformer;
import z.zer.tor.media.search.SearchError;
import z.zer.tor.media.search.SearchListener;
import z.zer.tor.media.search.SearchManager;
import z.zer.tor.media.search.SearchResult;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer;
import z.zer.tor.media.util.Utils;

public final class LocalSearchEngine {

    private final SearchManager manager;
    private SearchListener listener;

    private static final Object instanceLock = new Object();
    private static LocalSearchEngine instance;
    private final HashSet<Integer> opened = new HashSet<>();
    private long currentSearchToken;
    private boolean searchFinished;

    public static LocalSearchEngine instance() {
        if (instance != null) {
            return instance;
        } else {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new LocalSearchEngine();
                }
            }
            return instance;
        }
    }

    private LocalSearchEngine() {
        this.manager = SearchManager.getInstance();
        this.manager.setListener(new SearchListener() {
            @Override
            public void onResults(long token, List<SearchResult> results) {
                LocalSearchEngine.this.onResults(token, results);
            }

            @Override
            public void onError(long token, SearchError error) {
                LocalSearchEngine.this.onErr(token, error);
            }

            @Override
            public void onStopped(long token) {
                LocalSearchEngine.this.onFinished(token);
            }
        });
    }

    public SearchListener getListener() {
        return listener;
    }

    public void setListener(SearchListener listener) {
        this.listener = listener;
    }

    public void performSearch(String query) {
        if (Utils.isNullOrEmpty(query, true)) {
            return;
        }
        manager.stop();
        currentSearchToken = Math.abs(System.nanoTime());
        searchFinished = false;
        manager.perform(new SoundCloudSearchPerformer("api.sndcdn.com", currentSearchToken, query, 1000));
    }

    public void cancelSearch() {
        manager.stop();
        currentSearchToken = 0;
        searchFinished = true;
    }

    public boolean isSearchFinished() {
        return searchFinished;
    }

    public void clearCache() {
        CrawlPagedWebSearchPerformer.clearCache();
    }

    public void markOpened(SearchResult sr, AbstractListAdapter adapter) {
        opened.add(sr.uid());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public boolean hasBeenOpened(SearchResult sr) {
        return sr != null && opened.contains(sr.uid());
    }

    private void onResults(long token, List<SearchResult> results) {
        if (token == currentSearchToken) { // one more additional protection
            if (listener != null) {
                listener.onResults(token, results);
            }
        }
    }

    private void onErr(long token, SearchError error) {
        listener.onError(token, error);
    }

    private void onFinished(long token) {
        if (token == currentSearchToken) {
            searchFinished = true;
            if (listener != null) {
                listener.onStopped(token);
            }
        }
    }

    private String sanitize(String str) {
        str = Html.fromHtml(str).toString();
        str = str.replaceAll("\\.torrent|www\\.|\\.com|\\.net|[\\\\\\/%_;\\-\\.\\(\\)\\[\\]\\n\\rÐ&~{}\\*@\\^'=!,¡|#ÀÁ]", " ");
        str = Utils.removeDoubleSpaces(str);

        return str.trim();
    }

    private String normalize(String token) {
        String norm = Normalizer.normalize(token, Normalizer.Form.NFKD);
        norm = norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        norm = norm.toLowerCase(Locale.US);

        return norm;
    }

    private Set<String> normalizeTokens(Set<String> tokens) {
        Set<String> normalizedTokens = new HashSet<>(0);

        for (String token : tokens) {
            String norm = normalize(token);
            normalizedTokens.add(norm);
        }

        return normalizedTokens;
    }

    private List<String> tokenize(String keywords) {
        keywords = sanitize(keywords);

        Set<String> tokens = new HashSet<>(Arrays.asList(keywords.toLowerCase(Locale.US).split(" ")));

        return new ArrayList<>(normalizeTokens(tokens));
    }
}
