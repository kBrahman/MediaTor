package z.zer.tor.media.search;

import java.util.List;

public interface SearchListener {

    void onResults(long token, List<? extends SearchResult> results);

    void onError(long token, SearchError error);

    void onStopped(long token);
}
