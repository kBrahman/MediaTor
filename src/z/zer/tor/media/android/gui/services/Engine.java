package z.zer.tor.media.android.gui.services;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.services.EngineService.EngineServiceBinder;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.util.Ref;

import static z.zer.tor.media.android.util.Asyncs.async;

public final class Engine {

    private static final ExecutorService MAIN_THREAD_POOL = new EngineThreadPool();
    private static final String TAG = Engine.class.getSimpleName();

    private EngineService service;

    private FWVibrator vibrator;

    // the startServices call is a special call that can be made
    // to early (relatively speaking) during the application startup
    // the creation of the service is not (and can't be) synchronized
    // with the main activity resume.
    private boolean pendingStartServices = false;
    private boolean wasShutdown;

    private Engine() {
    }

    public boolean wasShutdown() {
        return wasShutdown;
    }

    private static class Loader {
        static final Engine INSTANCE = new Engine();
    }

    public static Engine instance() {
        return Engine.Loader.INSTANCE;
    }


    public boolean isStarted() {
        return service != null;
    }

    public boolean isStarting() {
        return service != null;
    }

    public boolean isStopped() {
        return service != null;
    }

    public boolean isStopping() {
        return service != null;
    }

    public boolean isDisconnected() {
        return service != null;
    }


    public ExecutorService getThreadPool() {
        return MAIN_THREAD_POOL;
    }

    public void notifyDownloadFinished(String displayName, File file, String optionalInfoHash) {
        if (service != null) {
            service.notifyDownloadFinished(file, optionalInfoHash);
        }
    }

    public void notifyDownloadFinished(String displayName, File file) {
        notifyDownloadFinished(displayName, file, null);
    }

    public void onHapticFeedbackPreferenceChanged() {
        if (vibrator != null) {
            vibrator.onPreferenceChanged();
        }
    }

    public void hapticFeedback() {
        if (vibrator != null) {
            vibrator.hapticFeedback();
        }
    }

    private static class FWVibrator {
        private final Vibrator vibrator;
        private boolean enabled;

        public FWVibrator(Application context) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            enabled = isActive();
        }

        public void hapticFeedback() {
            if (!enabled) return;
            try {
                vibrator.vibrate(50);
            } catch (Throwable ignored) {
            }
        }

        public void onPreferenceChanged() {
            enabled = isActive();
        }

        public boolean isActive() {
            boolean hapticFeedback = false;
            ConfigurationManager cm = ConfigurationManager.instance();
            if (cm != null) {
                hapticFeedback = cm.getBoolean(Constants.PREF_KEY_GUI_HAPTIC_FEEDBACK_ON);
            }
            return vibrator != null && hapticFeedback;
        }
    }

}
