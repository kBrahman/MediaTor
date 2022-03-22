package z.zer.tor.media.search;

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
    public void perform() throws IOException {
        for (int i = 1; !isStopped() && i <= pages; i++) {
            onResults(parsePage(i));
        }
    }

    protected List<SearchResult> parsePage(int page) throws IOException {
        List<SearchResult> result = Collections.emptyList();
        String url = getUrl(page, getEncodedKeywords());
        String text = fetch(url, null, null);
        Log.i(TAG, "fetched text=>" + text);
        if (text != null) {
            result = parsePage(text);
        }
        return result;
    }

    protected abstract String getUrl(int page, String encodedKeywords);

    protected abstract List<SearchResult> parsePage(String page);
}
