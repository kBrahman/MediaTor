package z.zer.tor.media.search.frostclick;

import z.zer.tor.media.util.Logger;
import z.zer.tor.media.search.PagedWebSearchPerformer;
import z.zer.tor.media.search.SearchResult;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrostClickSearchPerformer extends PagedWebSearchPerformer {

    private static final Logger LOG = Logger.getLogger(FrostClickSearchPerformer.class);

    private static final int MAX_RESULTS = 1;

    private final Map<String, String> customHeaders;

    public FrostClickSearchPerformer(String domainName, long token, String keywords, int timeout, UserAgent userAgent) {
        super(domainName ,token, keywords, timeout, MAX_RESULTS);
        this.customHeaders = buildCustomHeaders(userAgent);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://api.frostclick.com/q?page=" + page + "&q=" + encodedKeywords;
    }

    @Override
    protected List<? extends SearchResult> parsePage(int page) {
        String url = getUrl(page, getEncodedKeywords());
        String text;
        try {
            text = fetch(url, null, customHeaders);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        
        if (text != null) {
            return parsePage(text);
        } else {
            LOG.warn("Page content empty for url: " + url);
            return Collections.emptyList();
        }
    }

    @Override
    protected List<? extends SearchResult> parsePage(String page) {
        return Collections.emptyList();
    }

    private Map<String, String> buildCustomHeaders(UserAgent userAgent) {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(userAgent.getHeadersMap());
        map.put("User-Agent", userAgent.toString());
        map.put("sessionId", userAgent.getUUID());

        return map;
    }
}
