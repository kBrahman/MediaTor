package z.zer.tor.media.search;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.Map;

import z.zer.tor.media.util.HttpClientFactory;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.UrlUtils;
import z.zer.tor.media.util.UserAgentGenerator;
import z.zer.tor.media.util.http.HttpClient;

public abstract class WebSearchPerformer extends AbstractSearchPerformer {

    private static final Logger LOG = Logger.getLogger(WebSearchPerformer.class);

    private static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();

    private static final String[] STREAMABLE_EXTENSIONS = new String[]{"mp3", "ogg", "wma", "wmv", "m4a", "aac", "flac", "mp4", "flv", "mov", "mpg", "mpeg", "3gp", "m4v", "webm"};

    private final String domainName;
    private final String keywords;
    private final String encodedKeywords;
    private final int timeout;
    private final HttpClient client;

    public WebSearchPerformer(String domainName, long token, String keywords, int timeout) {
        super(token);

        if (domainName == null) {
            throw new IllegalArgumentException("domainName can't be null");
        }

        this.domainName = domainName;
        this.keywords = keywords;
        this.encodedKeywords = UrlUtils.encode(keywords);
        this.timeout = timeout;
        this.client = HttpClientFactory.getInstance(HttpClientFactory.HttpContext.SEARCH);
    }

    public final String getKeywords() {
        return keywords;
    }

    public final String getEncodedKeywords() {
        return encodedKeywords;
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        LOG.warn("Review your logic, calling deep search without implementation for: " + sr);
    }

    public String fetch(String url) throws IOException {
        return fetch(url, null, null);
    }

    public String fetch(String url, String cookie, Map<String, String> customHeaders) throws IOException {
        return client.get(url, timeout, DEFAULT_USER_AGENT, null, cookie, customHeaders);
    }

    public String post(String url, Map<String, String> formData) {
        try {
            return client.post(url, timeout, DEFAULT_USER_AGENT, formData);
        } catch (IOException throwable) {
            return null;
        }
    }

    /**
     * Allow to perform the HTTP operation using the same internal http client.
     *
     * @param url
     * @return the raw bytes from the http connection
     */
    public final byte[] fetchBytes(String url) {
        return fetchBytes(url, null, timeout);
    }

    protected final byte[] fetchBytes(String url, String referrer, int timeout) {
        if (url.startsWith("htt")) { // http(s)
            return client.getBytes(url, timeout, DEFAULT_USER_AGENT, referrer);
        } else {
            return null;
        }
    }

    public static boolean isStreamable(String filename) {
        String ext = FilenameUtils.getExtension(filename);
        for (String s : STREAMABLE_EXTENSIONS) {
            if (s.equals(ext)) {
                return true; // fast return
            }
        }

        return false;
    }

    public String getDomainName() {
        return domainName;
    }
}
