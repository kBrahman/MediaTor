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


    private final String encodedKeywords;
    private final int timeout;
    private final HttpClient client;

    public WebSearchPerformer(String domainName, long token, String keywords, int timeout) {
        super(token);

        if (domainName == null) {
            throw new IllegalArgumentException("domainName can't be null");
        }
        this.encodedKeywords = UrlUtils.encode(keywords);
        this.timeout = timeout;
        this.client = HttpClientFactory.getInstance(HttpClientFactory.HttpContext.SEARCH);
    }

    public final String getEncodedKeywords() {
        return encodedKeywords;
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        LOG.warn("Review your logic, calling deep search without implementation for: " + sr);
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

}
