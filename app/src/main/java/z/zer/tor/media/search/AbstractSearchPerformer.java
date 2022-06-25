package z.zer.tor.media.search;

import java.util.List;

import z.zer.tor.media.util.Logger;


public abstract class AbstractSearchPerformer implements SearchPerformer {

    private static final Logger LOG = Logger.getLogger(AbstractSearchPerformer.class);

    private final long token;

    private SearchListener listener;
    private boolean stopped;

    public AbstractSearchPerformer(long token) {
        this.token = token;
        this.stopped = false;
    }

    @Override
    public long getToken() {
        return token;
    }

    @Override
    public void stop() {
        stopped = true;
        try {
            if (listener != null) {
                listener.onStopped(token);
            }
        } catch (Throwable e) {
            LOG.warn("Error sending finished signal to listener: " + e.getMessage());
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public SearchListener getListener() {
        return listener;
    }

    @Override
    public void setListener(SearchListener listener) {
        this.listener = listener;
    }

    protected void onResults(List<SearchResult> results) {
        try {
            if (results != null && !stopped) {
                listener.onResults(token, results);
            }
        } catch (Throwable e) {
            LOG.warn("Error sending results to listener: " + e.getMessage());
        }
    }
}
