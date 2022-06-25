package z.zer.tor.media.search;

import java.io.IOException;

public interface SearchPerformer {

    long getToken();

    void perform() throws IOException;

    void crawl(CrawlableSearchResult sr);

    void stop();

    boolean isStopped();

    SearchListener getListener();

    void setListener(SearchListener listener);
}
