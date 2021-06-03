package z.zer.tor.media.search;

public interface SearchPerformer {

    long getToken();

    void perform();

    void crawl(CrawlableSearchResult sr);

    void stop();

    boolean isStopped();

    SearchListener getListener();

    void setListener(SearchListener listener);
}
