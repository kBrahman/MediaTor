package z.zer.tor.media.android.gui;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static z.zer.tor.media.android.core.Constants.MEDIA_TOR_NOTIFICATION_CHANNEL_ID;
import static z.zer.tor.media.android.util.Asyncs.async;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.android.gui.transfers.TransferManager;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.gui.views.TimerObserver;
import z.zer.tor.media.android.gui.views.TimerService;
import z.zer.tor.media.android.gui.views.TimerSubscription;
import z.zer.tor.media.util.Logger;

public final class NotificationUpdateDemon implements TimerObserver {

    private static final Logger LOG = Logger.getLogger(NotificationUpdateDemon.class);
    private static final int FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS = 5;
    private final Context mParentContext;
    private TimerSubscription mTimerSubscription;

    private RemoteViews notificationViews;
    private Notification notificationObject;

    public NotificationUpdateDemon(Context parentContext) {
        mParentContext = parentContext;
        setupNotification();
    }

    public void start() {
        if (mTimerSubscription != null) {
            LOG.debug("Stopping before (re)starting permanent notification demon");
            mTimerSubscription.unsubscribe();
        }
        mTimerSubscription = TimerService.subscribe(this, FROSTWIRE_STATUS_NOTIFICATION_UPDATE_INTERVAL_IN_SECS);
    }

    public void stop() {
        LOG.debug("Stopping permanent notification demon");
        mTimerSubscription.unsubscribe();

        NotificationManager manager = (NotificationManager) mParentContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.cancel(Constants.NOTIFICATION_FROSTWIRE_STATUS);
            } catch (SecurityException t) {
                // possible java.lang.SecurityException
            }
        }
    }

    private void updatePermanentStatusNotification() {
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_ENABLE_PERMANENT_STATUS_NOTIFICATION)) {
            return;
        }

        if (notificationViews == null || notificationObject == null) {
            LOG.warn("Notification views or object are null, review your logic");
            return;
        }

        // number of uploads (seeding) and downloads
        TransferManager transferManager;

        try {
            transferManager = TransferManager.instance();
        } catch (IllegalStateException btEngineNotReadyException) {
            return;
        }

        if (transferManager != null) {
            int downloads = transferManager.getActiveDownloads();
            int uploads = transferManager.getActiveUploads();
            if (downloads == 0 && uploads == 0) {
                NotificationManager manager = (NotificationManager) mParentContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    try {
                        manager.cancel(Constants.NOTIFICATION_FROSTWIRE_STATUS);
                    } catch (SecurityException ignored) {
                        // possible java.lang.SecurityException
                    }
                }
                return; // quick return
            }
            //  format strings
            String sDown = UIUtils.rate2speed(transferManager.getDownloadsBandwidth() / 1024);
            String sUp = UIUtils.rate2speed(transferManager.getUploadsBandwidth() / 1024);
            // Transfers status.
            notificationViews.setTextViewText(R.id.view_permanent_status_text_downloads, downloads + " @ " + sDown);
            notificationViews.setTextViewText(R.id.view_permanent_status_text_uploads, uploads + " @ " + sUp);
            final NotificationManager notificationManager = (NotificationManager) mParentContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(MEDIA_TOR_NOTIFICATION_CHANNEL_ID, "MediaTor", NotificationManager.IMPORTANCE_MIN);
                        channel.setSound(null, null);
                        notificationManager.createNotificationChannel(channel);
                    }
                    notificationManager.notify(Constants.NOTIFICATION_FROSTWIRE_STATUS, notificationObject);
                } catch (SecurityException ignored) {
                    // possible java.lang.SecurityException
                    ignored.printStackTrace();
                } catch (Throwable ignored2) {
                    // possible android.os.TransactionTooLargeException
                    ignored2.printStackTrace();
                }
            }
        }
    }

    private void setupNotification() {
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_ENABLE_PERMANENT_STATUS_NOTIFICATION)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(MEDIA_TOR_NOTIFICATION_CHANNEL_ID, "media_tor_channel", IMPORTANCE_DEFAULT);
            ((NotificationManager) mParentContext.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mChannel);
        }
        RemoteViews remoteViews = new RemoteViews(mParentContext.getPackageName(), R.layout.view_permanent_status_notification);

        PendingIntent showFrostWireIntent = createShowFrostwireIntent();
        PendingIntent shutdownIntent = createShutdownIntent();

        remoteViews.setOnClickPendingIntent(R.id.view_permanent_status_shutdown, shutdownIntent);
        remoteViews.setOnClickPendingIntent(R.id.view_permanent_status_text_title, showFrostWireIntent);
        Notification notification = new NotificationCompat.Builder(mParentContext, MEDIA_TOR_NOTIFICATION_CHANNEL_ID).
                setSmallIcon(R.mipmap.ic_launcher_round).
                setContentIntent(showFrostWireIntent).
                setContent(remoteViews).
                build();

        notification.flags |= Notification.FLAG_NO_CLEAR;

        notificationViews = remoteViews;
        notificationObject = notification;
    }

    public Notification getNotificationObject() {
        return notificationObject;
    }

    private PendingIntent createShowFrostwireIntent() {
        return PendingIntent.getActivity(mParentContext, 0, new Intent(mParentContext, MainActivity.class).
                setAction(Constants.ACTION_SHOW_TRANSFERS).
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK), PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent createShutdownIntent() {
        return PendingIntent.getActivity(mParentContext, 1, new Intent(mParentContext, MainActivity.class).
                setAction(Constants.ACTION_REQUEST_SHUTDOWN).
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK), PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onTime() {
        async(this, NotificationUpdateDemon::onTimeRefresh);
    }

    @SuppressWarnings("deprecation")
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) mParentContext.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isScreenOn();
    }

    private void onTimeRefresh() {
        if (isScreenOn()) {
            updatePermanentStatusNotification();
        }
    }
}
