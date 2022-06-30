package z.zer.tor.media.android.gui.util;

import android.widget.AbsListView;

public final class ScrollListeners {

    public static final class ComposedOnScrollListener implements AbsListView.OnScrollListener {
        private final AbsListView.OnScrollListener[] listeners;

        public ComposedOnScrollListener(AbsListView.OnScrollListener... listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
            if (listeners != null && listeners.length > 0) {
                for (AbsListView.OnScrollListener listener : listeners) {
                    try {
                        listener.onScrollStateChanged(absListView, i);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            if (listeners != null && listeners.length > 0) {
                for (AbsListView.OnScrollListener listener : listeners) {
                    try {
                        listener.onScroll(absListView, i, i1, i2);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    public static class FastScrollDisabledWhenIdleOnScrollListener implements AbsListView.OnScrollListener {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            absListView.setFastScrollEnabled(scrollState != SCROLL_STATE_IDLE);
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i1, int i2) {

        }
    }
}
