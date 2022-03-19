package z.zer.tor.media.android.gui.services;


import static z.zer.tor.media.android.util.Asyncs.async;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;

import java.io.File;

import okhttp3.ConnectionPool;
import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.android.gui.transfers.TransferManager;
import z.zer.tor.media.util.Hex;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.http.OKHTTPClient;

public class EngineService extends Service  {
    private static final Logger LOG = Logger.getLogger(EngineService.class);
    private final static long[] VENEZUELAN_VIBE = buildVenezuelanVibe();

    private static final String SHUTDOWN_ACTION = "com.frostwire.android.engine.SHUTDOWN";

    private final IBinder binder;
    // services in background
    private byte state;

    private NotifiedStorage notifiedStorage;

    public EngineService() {
        binder = new EngineServiceBinder();
    }

    @Override
    public void onCreate() {
        notifiedStorage = new NotifiedStorage(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        async(this, EngineService::cancelAllNotificationsTask);
        if (intent == null) {
            return START_NOT_STICKY;
        }
        if (SHUTDOWN_ACTION.equals(intent.getAction())) {
            LOG.info("onStartCommand() - Received SHUTDOWN_ACTION");
            new Thread("EngineService-onStartCommand(SHUTDOWN_ACTION) -> shutdownSupport") {
                @Override
                public void run() {
                    shutdownSupport();
                }
            }.start();
            return START_NOT_STICKY;
        }
        LOG.info("FrostWire's EngineService started by this intent:");
        LOG.info("FrostWire:" + intent.toString());
        LOG.info("FrostWire: flags:" + flags + " startId: " + startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.debug("onDestroy");
    }

    private void shutdownSupport() {
        LOG.debug("shutdownSupport");
        //enableComponentsTask(this, false); // we're already on a shutdown thread
        cancelAllNotificationsTask(this);
        stopServices();

        //ImageLoader.getInstance(this).shutdown();
        stopOkHttp();
        stopForeground(true);
    }

    // what a bad design to properly shutdown the framework threads!
    // TODO: deal with potentially active connections
    private void stopOkHttp() {
        ConnectionPool pool = OKHTTPClient.CONNECTION_POOL;
        try {
            pool.evictAll();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            synchronized (OKHTTPClient.CONNECTION_POOL) {
                pool.notifyAll();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public byte getState() {
        return state;
    }

    public synchronized void stopServices() {
        LOG.info("Pausing BTEngine...");
        TransferManager.instance().onShutdown();
        LOG.info("Pausing BTEngine paused");

        LOG.info("Engine stopped, state: " + state);
    }

    public void notifyDownloadFinished(File file, String infoHash) {
        try {
            if (notifiedStorage.contains(infoHash)) {
                // already notified
                return;
            } else {
                notifiedStorage.add(infoHash);
            }

            Context context = getApplicationContext();
            Intent i = new Intent(context, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION, true);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH, file.getAbsolutePath());
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context, Constants.MEDIA_TOR_NOTIFICATION_CHANNEL_ID).setWhen(System.currentTimeMillis()).setContentText(getString(R.string.download_finished)).setContentTitle(getString(R.string.download_finished)).setSmallIcon(R.mipmap.ic_launcher_round).setContentIntent(pi).build();
            notification.vibrate = ConfigurationManager.instance().vibrateOnFinishedDownload() ? VENEZUELAN_VIBE : null;
            notification.number = TransferManager.instance().getDownloadsToReview();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            if (manager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(Constants.MEDIA_TOR_NOTIFICATION_CHANNEL_ID, "FrostWire", NotificationManager.IMPORTANCE_MIN);
                    channel.setSound(null, null);
                    manager.createNotificationChannel(channel);
                }
                manager.notify(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED, notification);
            }
        } catch (Throwable e) {
            LOG.error("Error creating notification for download finished", e);
        }
    }

    public class EngineServiceBinder extends Binder {
        public EngineService getService() {
            return EngineService.this;
        }
    }

    private static long[] buildVenezuelanVibe() {

        long shortVibration = 80;
        long mediumVibration = 100;
        long shortPause = 100;
        long mediumPause = 150;
        long longPause = 180;

        return new long[]{0, shortVibration, longPause, shortVibration, shortPause, shortVibration, shortPause, shortVibration, mediumPause, mediumVibration};
    }

    private static final class NotifiedStorage {

        // this is a preference key to be used only by this class
        private static final String PREF_KEY_NOTIFIED_HASHES = "frostwire.prefs.gui.notified_hashes";

        // not using ConfigurationManager to avoid setup/startup timing issues
        private final SharedPreferences preferences;

        NotifiedStorage(Context context) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        public boolean contains(String infoHash) {
            if (infoHash == null || infoHash.length() != 40) {
                // not a valid info hash
                return false;
            }

            return false;
        }

        public void add(String infoHash) {
            if (infoHash == null || infoHash.length() != 40) {
                // not a valid info hash
                return;
            }

            try {

                byte[] arr = Hex.decode(infoHash);
                String s = Hex.encode(arr);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_KEY_NOTIFIED_HASHES, s);
                editor.apply();

            } catch (Throwable e) {
                LOG.warn("Error adding info hash to notified storage", e);
            }
        }
    }

    private static void cancelAllNotificationsTask(EngineService engineService) {
        try {
            NotificationManager notificationManager = (NotificationManager) engineService.getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
        } catch (SecurityException ignore) {
            // new exception in Android 7
        }
    }
}
