package z.zer.tor.media.android.gui.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.andrew.apollo.utils.MusicUtils
import com.facebook.ads.*
import org.json.JSONObject
import z.zer.tor.media.R
import z.zer.tor.media.android.core.Constants
import z.zer.tor.media.android.gui.dialogs.NewTransferDialog
import z.zer.tor.media.android.gui.services.Engine
import z.zer.tor.media.android.gui.views.AbstractDialog
import z.zer.tor.media.util.Ref
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class PlayerActivity : AppCompatActivity(), AbstractDialog.OnDialogClickListener,
    MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener,
    AudioManager.OnAudioFocusChangeListener, SeekBar.OnSeekBarChangeListener, Runnable {

    private lateinit var adLoaded: MutableState<Boolean>
    val TAG = PlayerActivity::class.java.simpleName
    var androidMediaPlayer: MediaPlayer? = null
    var displayName: String? = null
    var source: String? = null
    var streamUrl: String? = null
    var isFullScreen = false
    var changedActionBarTitleToNonBuffering = false
    val handler = Handler(Looper.getMainLooper())
    private lateinit var progress: MutableState<Float>
    private lateinit var playerStarted: MutableState<Boolean>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.application_label)
        val displayName = intent.getStringExtra("displayName").toString()
        val nativeAd = NativeAd(this, getString(R.string.id_ad_native_fb))
        nativeAd.loadAd(nativeAd.buildLoadAdConfig().withAdListener(object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad) {
                Log.e(TAG, "Native ad finished downloading all assets.")
            }

            override fun onError(ad: Ad, adError: AdError) {
                // Native ad failed to load
                Log.e(TAG, "Native ad failed to load: " + adError.errorMessage)
            }

            override fun onAdLoaded(ad: Ad) {
                if (nativeAd !== ad) {
                    return
                }
                // Inflate Native Ad into Container
                adLoaded.value = true
//                inflateAd(nativeAd)
            }

            override fun onAdClicked(ad: Ad) {
                // Native ad clicked
                Log.d(TAG, "Native ad clicked!")
            }

            override fun onLoggingImpression(ad: Ad) {
                // Native ad impression
                Log.d(TAG, "Native ad impression logged!")
            }
        }).build())

        this.setContent {
            val playing = remember { mutableStateOf(true) }
            progress = remember { mutableStateOf(0F) }
            playerStarted = remember { mutableStateOf(false) }
            adLoaded = remember { mutableStateOf(false) }
            var w = 0
            val colorPrimary = Color(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getColor(R.color.colorPrimary)
                } else {
                    resources.getColor(R.color.colorPrimary)
                }
            )
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (btn, sb, row, col, mv, socialCtx, bodyTxt, spacer) = createRefs()
                Column(
                    Modifier
                        .padding(8.dp)
                        .constrainAs(col) {}) {
                    Text(text = displayName, fontSize = 20.sp)
                    Text(text = intent.getStringExtra("source").toString())

                }

                if (adLoaded.value) {
                    val icon = MediaView(this@PlayerActivity)
                    Row(Modifier
                        .constrainAs(row) {
                            top.linkTo(col.bottom, margin = 10.dp)
                        }
                        .padding(8.dp)) {
                        AndroidView(modifier = Modifier.size(35.dp), factory = { icon })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = nativeAd.advertiserName.toString(), fontSize = 15.sp)
                            Text(
                                text = nativeAd.sponsoredTranslation.toString(),
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(Modifier.weight(1F))
                        AndroidView(factory = {
                            AdOptionsView(
                                this@PlayerActivity,
                                nativeAd,
                                null
                            )
                        })
                    }
                    AndroidView(modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .constrainAs(mv) {
                            top.linkTo(row.bottom)
                            bottom.linkTo(socialCtx.top, margin = 4.dp)
                            height = Dimension.fillToConstraints
                        }, factory = {
                        val mediaView = MediaView(this@PlayerActivity)
                        nativeAd.registerViewForInteraction(mediaView, mediaView, icon)
                        mediaView
                    })
                    Text(text = nativeAd.adSocialContext.toString(), modifier = Modifier
                        .constrainAs(socialCtx) {
                            bottom.linkTo(bodyTxt.top, margin = 4.dp)
                        }
                        .padding(start = 4.dp))
                    Text(text = nativeAd.adBodyText.toString(), modifier = Modifier
                        .constrainAs(bodyTxt) {
                            bottom.linkTo(spacer.top, margin = 4.dp)
                        }
                        .padding(start = 4.dp, end = 4.dp))
                }
                Spacer(
                    Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .constrainAs(spacer) {
                            bottom.linkTo(btn.top)
                        })
                if (playerStarted.value) {
                    Button(onClick = {
                        if (playing.value) {
                            androidMediaPlayer?.pause()
                            playing.value = false
                        } else {
                            androidMediaPlayer?.start()
                            playing.value = true
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = colorPrimary),
                        modifier = Modifier
                            .width(48.dp)
                            .constrainAs(btn) {
                                start.linkTo(parent.start, margin = 8.dp)
                                bottom.linkTo(parent.bottom, margin = 8.dp)
                            }) {
                        Icon(
                            painter = painterResource(id = if (playing.value) R.drawable.ic_pause_24 else R.drawable.ic_play_24),
                            contentDescription = "",
                            tint = Color.White
                        )
                    }

                    LinearProgressIndicator(progress = progress.value,
                        color = colorPrimary,
                        modifier = Modifier
                            .height(8.dp)
                            .constrainAs(sb) {
                                start.linkTo(btn.end, margin = 4.dp)
                                end.linkTo(parent.end, margin = 16.dp)
                                top.linkTo(btn.top)
                                bottom.linkTo(btn.bottom)
                                width = Dimension.fillToConstraints
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                w = placeable.width
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    progress.value = it.x / w
                                    androidMediaPlayer?.seekTo(
                                        (progress.value * (androidMediaPlayer?.duration
                                            ?: 0)).toInt()
                                    )
                                }
                            })
                } else {
                    LinearProgressIndicator(
                        color = colorPrimary,
                        modifier = Modifier.constrainAs(sb) {
                            bottom.linkTo(parent.bottom, margin = 16.dp)
                            start.linkTo(parent.start, margin = 16.dp)
                            end.linkTo(parent.end, margin = 16.dp)
                            width = Dimension.fillToConstraints
                        })
                }
            }
        }
        initComponents()

    }

    fun initComponents() {
        val i: Intent = intent
        stopAnyOtherPlayers()
        source = i.getStringExtra("source")
        streamUrl = i.getStringExtra("streamUrl")
        if (streamUrl == null) {
            Toast.makeText(this, R.string.media_player_failed, LENGTH_SHORT).show()
            finish()
            return
        }
        isFullScreen = i.getBooleanExtra("isFullScreen", false)
        play()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("displayName", displayName)
        outState.putString("source", source)
        outState.putString("streamUrl", streamUrl)
        outState.putBoolean("isFullScreen", isFullScreen)
        if (androidMediaPlayer != null && androidMediaPlayer!!.isPlaying) {
            outState.putInt("currentPosition", androidMediaPlayer!!.currentPosition)
        }
    }

    fun getFinalUrl(url: String?): String? {
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
                    // ignore
                }
            }
        }
        return url
    }

    override fun onDialogClick(tag: String, which: Int) {
        if (tag == NewTransferDialog.TAG && which == Dialog.BUTTON_POSITIVE) {
            if (Ref.alive(NewTransferDialog.srRef)) {
                releaseMediaPlayer()
                val i = Intent(this, MainActivity::class.java)
                i.action = Constants.ACTION_START_TRANSFER_FROM_PREVIEW
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(i)
            }
            finish()
        }
    }

    private fun releaseMediaPlayer() {
        if (androidMediaPlayer != null) {
            androidMediaPlayer!!.stop()
            androidMediaPlayer!!.setSurface(null)
            try {
                androidMediaPlayer!!.release()
            } catch (t: Throwable) {
                //there could be a runtime exception thrown inside stayAwake()
            }
            androidMediaPlayer = null
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            audioManager?.abandonAudioFocus(this)
        }
    }

    fun play() {
        val t: Thread = object : Thread("PreviewPlayerActivity-onSurfaceTextureAvailable") {
            override fun run() {
                Log.i(TAG, "url=>$streamUrl")
                val url = getFinalUrl(streamUrl)
                val uri = Uri.parse(url)
                androidMediaPlayer = MediaPlayer()
                try {
                    androidMediaPlayer!!.setDataSource(this@PlayerActivity, uri)
                    androidMediaPlayer!!.setOnBufferingUpdateListener(this@PlayerActivity)
                    androidMediaPlayer!!.setOnCompletionListener(this@PlayerActivity)
                    androidMediaPlayer!!.setOnPreparedListener(this@PlayerActivity)
                    androidMediaPlayer!!.setOnInfoListener(this@PlayerActivity)
                    androidMediaPlayer!!.prepare()
                    startSeekBar()
                    androidMediaPlayer!!.start()
                    playerStarted.value = true
                    if (MusicUtils.isPlaying()) {
                        MusicUtils.playOrPause()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        t.start()
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {}

    override fun onCompletion(mp: MediaPlayer?) {
        finish()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        am?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        var startedPlayback = false
        when (what) {
            MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> {
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_END ->                 //LOG.warn("End of media buffering.");
                startedPlayback = true
            MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> {
            }
            MediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {
            }
            MediaPlayer.MEDIA_INFO_METADATA_UPDATE -> {
            }
            MediaPlayer.MEDIA_INFO_UNKNOWN -> {
            }
            else -> {
            }
        }
        if (startedPlayback && !changedActionBarTitleToNonBuffering) {
            changedActionBarTitleToNonBuffering = true
        }
        return false
    }

    fun stopAnyOtherPlayers() {
        try {
            val mediaPlayer = Engine.instance().mediaPlayer
            if (mediaPlayer != null && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        } catch (ignored: Throwable) {
        }
        val mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        if (mAudioManager != null && mAudioManager.isMusicActive) {
            val i = Intent("com.android.music.musicservicecommand")
            i.putExtra("command", "pause")
            application.sendBroadcast(i)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(this)
        stopAnyOtherPlayers()
        releaseMediaPlayer()
        super.onDestroy()
    }


    override fun onPause() {
        releaseMediaPlayer()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        changedActionBarTitleToNonBuffering = false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            releaseMediaPlayer()
        }
    }

    fun startSeekBar() {
        handler.postDelayed(this, 1000)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            seekBar.progress = progress
            androidMediaPlayer!!.seekTo(progress * androidMediaPlayer!!.duration / 100)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    override fun run() {
        if (androidMediaPlayer == null) return
        val currentPosition = androidMediaPlayer!!.currentPosition
        progress.value = currentPosition.toFloat() / androidMediaPlayer!!.duration
        startSeekBar()
    }
}