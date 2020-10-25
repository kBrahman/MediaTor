package zig.zak.media.tor.search;

import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public abstract class PagedWebSearchPerformer extends WebSearchPerformer {

    private static final String TAG = PagedWebSearchPerformer.class.getSimpleName();

    private final int pages;

    public PagedWebSearchPerformer(String domainName, long token, String keywords, int timeout, int pages) {
        super(domainName, token, keywords, timeout);
        this.pages = pages;
    }

    @Override
    public void perform() {
        for (int i = 1; !isStopped() && i <= pages; i++) {
            onResults(searchPage(i));
        }
    }

    protected List<? extends SearchResult> searchPage(int page) {
        List<? extends SearchResult> result = Collections.emptyList();
        String url = null;
        try {
            url = getUrl(page, getEncodedKeywords());
            String text = fetchSearchPage(url);
            Log.i(TAG, "txt=>" + text);
            if (text != null) {
                result = searchPage(text);
            }
        } catch (Throwable e) {
            if (url == null) {
                url = "n.a";
            }
            e.printStackTrace();
            Log.e(TAG, "Error searching page [" + url + "]: " + e.getMessage());
        }
        return result;
    }

    protected String fetchSearchPage(String url) throws IOException {
        return fetch(url);
    }

    protected abstract String getUrl(int page, String encodedKeywords);

    protected abstract List<? extends SearchResult> searchPage(String page);
}
