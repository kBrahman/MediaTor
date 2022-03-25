package z.zer.tor.media.android.gui.services

import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getService
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import z.zer.tor.media.App
import z.zer.tor.media.R
import z.zer.tor.media.android.db.Db
import z.zer.tor.media.android.db.PlayTrack
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer.SOUND_CLOUD_CLIENT_ID
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class PlayService : Service() {

    companion object {
        private const val TAG = "PlayService"
        private const val CHANNEL_ID = "media_play_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PLAY_PAUSE = "action.PLAY_PAUSE"
        private const val ACTION_PREV = "action.PREV"
        private const val ACTION_NEXT = "action.NEXT"
        private const val ACTION_STOP_SERVICE = "action.STOP_SERVICE"
    }


    private lateinit var builder: NotificationCompat.Builder
    private var tracks = emptyList<PlayTrack>()
    private val binder = PlayBinder()
    private lateinit var notManager: NotificationManagerCompat
    private var index = 0
    private lateinit var db: Db
    private val cScope = CoroutineScope(Dispatchers.IO)
    private val mp = MediaPlayer()
    private var repeat = false
    private var repeatAll = false

    override fun onCreate() {
        super.onCreate()
        db = (application as App).db
        val style = androidx.media.app.NotificationCompat.MediaStyle()
        style.setShowActionsInCompactView(0, 1, 2, 3)
        style.setCancelButtonIntent(getThisService(ACTION_STOP_SERVICE))
        builder = NotificationCompat.Builder(this, CHANNEL_ID).setStyle(style).setVisibility(VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification).setColorized(true).setColor(getColor(R.color.colorPrimary))
            .addAction(getAction(R.drawable.ic_previous_24, ACTION_PREV))
            .addAction(getAction(R.drawable.ic_pause_24, ACTION_PLAY_PAUSE))
            .addAction(getAction(R.drawable.ic_next_24, ACTION_NEXT))
            .addAction(getAction(R.drawable.contextmenu_icon_remove_transfer, ACTION_STOP_SERVICE))


        notManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notManager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ID, IMPORTANCE_LOW)
                    .apply { setName("MediaPlay Notifications") }.build()
            )
        }
        mp.setOnCompletionListener {
            Log.i(TAG, "player finish")
            if (repeat) mp.start()
            else if (repeatAll) {
                val pos = if (index == tracks.size - 1) 0 else index + 1
                binder.listener?.showProgress(true)
                onStartCommand(Intent().apply { putExtra("index", pos) }, START_FLAG_RETRY, 1)
            } else {
                binder.listener?.showPlayer(false)
                stopForeground(true)
                stopSelf()
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "on start command, action=>${intent?.action}")
        if (intent?.action != null) {
            when (intent.action) {
                ACTION_PLAY_PAUSE -> {
                    if (mp.isPlaying) binder.pause() else binder.play()
                    binder.listener?.setPlaying(mp.isPlaying)
                }
                ACTION_STOP_SERVICE -> {
                    mp.stop()
                    mp.reset()
                    binder.listener?.showPlayer(false)
                    stopForeground(true)
                    stopSelf()
                    Log.i(TAG, "service stopped")
                }
                else -> {
                    if ((intent.action == ACTION_NEXT && index == tracks.size - 1) ||
                        (intent.action == ACTION_PREV && index == 0)
                    ) return super.onStartCommand(intent, flags, startId)
                    val pos = index + if (intent.action == ACTION_NEXT) 1 else -1
                    binder.listener?.showProgress(true)
                    mp.reset()
                    onStartCommand(intent.apply {
                        putExtra("index", pos)
                        action = null
                    }, START_FLAG_RETRY, 1)
                }
            }
            return super.onStartCommand(intent, flags, startId)
        }
        index = intent?.getIntExtra("index", 0) ?: 0
        cScope.launch {
            if (tracks.isEmpty() || index >= tracks.size) tracks = db.trackDao().all()
            val t = tracks[index]
            Log.i(TAG, t.url)
            val path = t.url.substringBefore("=") + "=" + SOUND_CLOUD_CLIENT_ID
            Log.i(TAG, "setting data source=>${mp.hashCode()}")
            mp.setDataSource(getFinalUrl(path))
            mp.prepare()
            mp.start()
            builder.setContentTitle(t.name)
            startForeground(NOTIFICATION_ID, builder.build())
            binder.listener?.setCurrItem(t)
            binder.listener?.showProgress(false)
            startProgress()
        }
        Log.i(TAG, "on start id=>$startId")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startProgress() {
        cScope.launch(Dispatchers.Default) {
            delay(1000)
            if (!mp.isPlaying) {
                return@launch
            }
            val progress = mp.currentPosition.toFloat() / mp.duration
            binder.listener?.onProgress(progress)
            startProgress()
        }
    }

    private fun getFinalUrl(url: String?): String? {
        var con: HttpURLConnection? = null
        try {
            con = URL(url).openConnection() as HttpURLConnection
            con.connect()
            val inputStream = con.inputStream
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val result = if (scanner.hasNext()) scanner.next() else ""
            return JSONObject(result).getString("url")
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            if (con != null) {
                try {
                    con.disconnect()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        return url
    }

    override fun onBind(p0: Intent?) = binder

    private fun getAction(@DrawableRes resId: Int, action: String) =
        NotificationCompat.Action.Builder(
            IconCompat.createWithResource(this, resId), action,
            getThisService(action)
        ).build()

    private fun getThisService(action: String) = getService(
        this, 0,
        Intent(action, null, this, PlayService::class.java),
        FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    inner class PlayBinder : Binder() {
        var listener: PlayListener? = null
        fun pause() {
            mp.pause()
            builder.mActions[1] = getAction(R.drawable.ic_play_24, ACTION_PLAY_PAUSE)
            notManager.notify(NOTIFICATION_ID, builder.build())
        }

        fun play() {
            mp.start()
            builder.mActions[1] = getAction(R.drawable.ic_pause_24, ACTION_PLAY_PAUSE)
            notManager.notify(NOTIFICATION_ID, builder.build())
            startProgress()
        }

        fun seekTo(value: Float) = mp.seekTo((value * mp.duration).toInt())
        fun setRepeat(repeat: Boolean) {
            this@PlayService.repeat = repeat
        }

        fun setRepeatAll(repeatAll: Boolean) {
            this@PlayService.repeatAll = repeatAll
        }

        fun resetMP() = mp.reset()
    }


    interface PlayListener {
        fun showProgress(b: Boolean)
        fun onProgress(progress: Float)
        fun setCurrItem(t: PlayTrack)
        fun setPlaying(playing: Boolean)
        fun showPlayer(b: Boolean)
    }
}



