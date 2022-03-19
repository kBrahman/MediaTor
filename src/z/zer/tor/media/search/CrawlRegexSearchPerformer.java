package z.zer.tor.media.search;

import java.util.Collections;
import java.util.List;


public abstract class CrawlRegexSearchPerformer<T extends CrawlableSearchResult> extends CrawlPagedWebSearchPerformer<T> implements RegexSearchPerformer<T> {

    private final int regexMaxResults;

    public CrawlRegexSearchPerformer(String domainName, long token, String keywords, int timeout, int pages, int numCrawls, int regexMaxResults) {
        super(domainName, token, keywords, timeout, pages, numCrawls);
        this.regexMaxResults = regexMaxResults;
    }

    @Override
    protected List<SearchResult> parsePage(String page) {
        if (!isValidHtml(page)) {
            return Collections.emptyList();
        }
        int prefixOffset = preliminaryHtmlPrefixOffset(page);
        int suffixOffset = preliminaryHtmlSuffixOffset(page);
        String reducedPage = PerformersHelper.reduceHtml(page, prefixOffset, suffixOffset);
        return PerformersHelper.searchPageHelper(this, reducedPage, regexMaxResults);
    }

    /**
     * Give the opportunity to an implementor to specify if the unreduced HTML
     * that is about to be crawled is a valid one, and not report errors when
     * there is none.
     *
     * @param html the unreduced html
     * @return {@code true} is valid and allowed to be processed, {@code false}
     * otherwise.
     */
    abstract protected boolean isValidHtml(String html);

    protected int preliminaryHtmlSuffixOffset(String page) {
        return page.length();
    }

    protected int preliminaryHtmlPrefixOffset(String page) {
        return 0;
    }
}
