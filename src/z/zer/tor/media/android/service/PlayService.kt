package z.zer.tor.media.android.service

import android.app.Notification
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.graphics.drawable.IconCompat
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import z.zer.tor.media.R
import z.zer.tor.media.android.db.Db

class PlayService : Service() {

    companion object {
        private const val TAG = "PlayService"
        private const val CHANNEL_ID = "media_play_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var notManager: NotificationManagerCompat
    private lateinit var notification: Notification
    private var index = 0
    private lateinit var db: Db
    private val cScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            this,
            Db::class.java,
            getString(R.string.application_label) + "_db"
        ).build()

        val prevIntent = null
        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setVisibility(VISIBILITY_PUBLIC).setSmallIcon(R.drawable.ic_notification)
                .addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(this, R.drawable.ic_previous_24),
                        "prev",
                        prevIntent
                    ).build()
                ).addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(this, R.drawable.ic_pause_24),
                        "play_pause",
                        prevIntent
                    ).build()
                ).addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(this, R.drawable.ic_next_24),
                        "next",
                        prevIntent
                    ).build()
                ).setContentTitle(getString(R.string.loading) + "...")
        notification = builder.build()

        notManager = NotificationManagerCompat.from(this)
//        notManager.createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL_ID, IMPORTANCE_LOW).build())
        cScope.launch {
            val tracks = db.trackDao().all()
            val t = tracks[index]
            val mp = MediaPlayer()
            mp.setDataSource(t.url)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        index = intent?.getIntExtra("index", 0) ?: 0
        startForeground(NOTIFICATION_ID, notification)
        notManager.notify(NOTIFICATION_ID, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?) = PlayBinder()

    class PlayBinder : Binder()
}

