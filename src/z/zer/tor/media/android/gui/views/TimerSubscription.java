package z.zer.tor.media.android.gui.views;

import android.app.Fragment;
import android.view.View;

import java.lang.ref.WeakReference;

import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

public final class TimerSubscription {

    private static final Logger LOG = Logger.getLogger(TimerSubscription.class);

    private final WeakReference<TimerObserver> observer;

    private boolean unsubscribed;

    public TimerSubscription(TimerObserver observer) {
        this.observer = Ref.weak(observer);

        this.unsubscribed = false;
    }

    public boolean isSubscribed() {
        if (!unsubscribed && !Ref.alive(observer)) {
            unsubscribe();
        }

        return !unsubscribed;
    }

    public void unsubscribe() {
        unsubscribed = true;
        Ref.free(observer);
    }

    public void onTime() {
        if (isSubscribed()) {
            try {
                onTime(observer.get());
            } catch (Throwable e) {
                unsubscribe();
                LOG.error("Error notifying observer, performed automatic unsubscribe", e);
            }
        }
    }

    private static void onTime(TimerObserver observer) {
        boolean call = true;
        if (observer instanceof View) {
            // light version of visible check
            call = ((View) observer).getVisibility() != View.GONE;
        }
        if (observer instanceof Fragment) {
            call = ((Fragment) observer).isVisible();
            if (observer instanceof AbstractFragment) {
                call = !((AbstractFragment) observer).isPaused();
            }
        }
        if (observer instanceof AbstractActivity) {
            call = !((AbstractActivity) observer).isPaused();
        }
        if (call) {
            observer.onTime();
            //LOG.debug("ON TIME: class-" + observer.getClass().getName());
        }
    }
}
