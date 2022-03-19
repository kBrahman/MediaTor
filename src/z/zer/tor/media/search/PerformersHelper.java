package z.zer.tor.media.search;


import java.util.LinkedList;
import java.util.List;

import z.zer.tor.media.regex.MediaPattern;
import z.zer.tor.media.search.torrent.TorrentCrawlableSearchResult;
import z.zer.tor.media.search.torrent.TorrentCrawledSearchResult;
import z.zer.tor.media.util.Logger;

public final class PerformersHelper {

    private static final Logger LOG = Logger.getLogger(PerformersHelper.class);

    private static final MediaPattern MAGNET_HASH_PATTERN = MediaPattern.compile("magnet\\:\\?xt\\=urn\\:btih\\:([a-fA-F0-9]{40})");

    private PerformersHelper() {
    }

    public static List<SearchResult> searchPageHelper(RegexSearchPerformer<?> performer, String page, int regexMaxResults) {
        List<SearchResult> result = new LinkedList<>();

        if (page == null) {
            LOG.warn(performer.getClass().getSimpleName() + " returning null page. Issue fetching page or issue getting page prefix/suffix offsets. Notify developers at contact@frostwire.com");
            return result;
        }

        SearchMatcher matcher = SearchMatcher.from(performer.getPattern().matcher(page));
        int i = 0;
        boolean matcherFound;

        do {
            try {
                matcherFound = matcher.find();
            } catch (Throwable t) {
                matcherFound = false;
                LOG.error(performer.getPattern().toString() + " has failed.\n" + t.getMessage(), t);
            }

            if (matcherFound) {
                SearchResult sr = performer.fromMatcher(matcher);
                if (sr != null) {
                    result.add(sr);
                    i++;
                }
            }
        } while (matcherFound && i < regexMaxResults && !performer.isStopped());

        return result;
    }

    /**
     * This method is only public allow reuse inside the package search, consider it a private API
     */
    public static List<? extends SearchResult> crawlTorrent(SearchPerformer performer, TorrentCrawlableSearchResult sr, byte[] data, boolean detectAlbums) {
        List<TorrentCrawledSearchResult> list = new LinkedList<>();

        if (data == null) {
            return list;
        }

        if (detectAlbums) {
            List<SearchResult> temp = new LinkedList<>();
            temp.addAll(list);
            temp.addAll(new AlbumCluster().detect(sr, list));
            return temp;
        } else {
            return list;
        }
    }

    public static List<? extends SearchResult> crawlTorrent(SearchPerformer performer, TorrentCrawlableSearchResult sr, byte[] data) {
        return crawlTorrent(performer, sr, data, false);
    }

    public static String parseInfoHash(String url) {
        String result = null;
        final SearchMatcher matcher = SearchMatcher.from(MAGNET_HASH_PATTERN.matcher(url));
        try {
            if (matcher.find()) {
                result = matcher.group(1);
            }
        } catch (Throwable t) {
            LOG.error("Could not parse magnet out of " + url, t);
        }
        return result;
    }

    public static String reduceHtml(String html, int prefixOffset, int suffixOffset) {
        if (prefixOffset == -1 || suffixOffset == -1) {
            html = null;
        } else if ((prefixOffset > 0 || suffixOffset < html.length())) {
            if (prefixOffset > suffixOffset) {
                LOG.warn("PerformersHelper.reduceHtml() Check your logic: prefixOffset:" + prefixOffset + " > suffixOffset:" + suffixOffset);
                LOG.info(html);
                return null;
            }

            html = new String(html.substring(prefixOffset, suffixOffset).toCharArray());
        }
        return html;
    }
}
