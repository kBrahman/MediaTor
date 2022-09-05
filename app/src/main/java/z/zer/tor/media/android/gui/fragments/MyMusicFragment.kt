package z.zer.tor.media.android.gui.fragments

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import com.facebook.ads.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import z.zer.tor.media.App
import z.zer.tor.media.BuildConfig
import z.zer.tor.media.R
import z.zer.tor.media.android.db.Db
import z.zer.tor.media.android.db.PlayTrack
import z.zer.tor.media.android.gui.services.PlayService
import java.io.IOException
import java.net.URL

@ExperimentalMaterialApi
class MyMusicFragment : Fragment(), ServiceConnection, PlayService.PlayListener {
    companion object {
        private const val TAG = "MyMusicFragment"
    }

    private lateinit var nativeAd: NativeAd
    private var adLoaded = mutableStateOf(false)
    private lateinit var db: Db
    private lateinit var playing: MutableState<Boolean>
    private lateinit var progress: MutableState<Float>
    private lateinit var currItem: PlayTrack
    private lateinit var showPlayer: MutableState<Boolean>
    private lateinit var showLoading: MutableState<Boolean>
    private var service: PlayService.PlayBinder? = null
    private var tracks: SnapshotStateList<PlayTrack> = mutableStateListOf()
    private val cScope = CoroutineScope(Dispatchers.IO)
    private val imageCache = HashMap<String?, Bitmap?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "on create")
        nativeAd = NativeAd(requireContext(), getString(R.string.id_ad_native_fb))
        if (BuildConfig.DEBUG) AdSettings.addTestDevice("2fc89f65-eb70-4b9d-aac2-d812bb0b95b3")
        nativeAd.loadAd(nativeAd.buildLoadAdConfig().withAdListener(object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad) {
                Log.e(TAG, "Native ad finished downloading all assets.")
            }

            override fun onError(ad: Ad, adError: AdError) {
                Log.e(TAG, "Native ad failed to load: " + adError.errorMessage)
            }

            override fun onAdLoaded(ad: Ad) {
                if (nativeAd !== ad) {
                    return
                }
                adLoaded.value = true
                Log.e(TAG, "Native ad is loaded")
            }

            override fun onAdClicked(ad: Ad) {
                Log.d(TAG, "Native ad clicked!")
            }

            override fun onLoggingImpression(ad: Ad) {
                // Native ad impression
                Log.d(TAG, "Native ad impression logged!")
            }
        }).build())
        db = (activity?.application as App).db
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(TAG, "onCreateView")
        return ComposeView(requireContext()).apply {
            val colorPrimary = Color(getColor(context, R.color.colorPrimary))
            setContent {
                MaterialTheme(colors = lightColors(colorPrimary)) {
                    tracks = remember<SnapshotStateList<PlayTrack>> { mutableStateListOf() }.apply { update() }
                    ConstraintLayout(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        val (bar, content) = createRefs()
                        val lightBlue = Color(getColor(context, R.color.basic_blue_highlight))
                        var w = 0
                        showPlayer = remember { mutableStateOf(false) }
                        showLoading = remember { mutableStateOf(false) }
                        playing = remember { mutableStateOf(false) }
                        progress = remember { mutableStateOf(0F) }
                        var repeatAll by remember { mutableStateOf(false) }
                        var repeat by remember { mutableStateOf(false) }
                        TopAppBar(Modifier.constrainAs(bar) {}) {
                            Text(getString(R.string.my_music))
                            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                IconButton(onClick = {
                                    repeat = !repeat
                                    repeatAll = false
                                    service?.setRepeat(repeat)
                                    service?.setRepeatAll(false)
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_repeat_one_24),
                                        getString(R.string.repeat),
                                        tint = if (repeat) lightBlue else Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    repeatAll = !repeatAll
                                    repeat = false
                                    service?.setRepeatAll(repeatAll)
                                    service?.setRepeat(false)
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_repeat_24),
                                        getString(R.string.repeat_all),
                                        tint = if (repeatAll) lightBlue else Color.White
                                    )
                                }
                            }
                        }
                        if (showLoading.value) {
                            LinearProgressIndicator(
                                Modifier
                                    .fillMaxWidth()
                                    .constrainAs(createRef()) {
                                        top.linkTo(bar.bottom)
                                    }, lightBlue, Color.White
                            )
                        }
                        LazyColumn(Modifier.constrainAs(content) {
                            top.linkTo(bar.bottom)
                        }, contentPadding = PaddingValues(4.dp)) {
                            itemsIndexed(items = tracks, key = { _, track -> track.id }) { i, item ->
                                val dismissState = rememberDismissState(confirmStateChange = {
                                    if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                                        tracks.removeAt(i)
                                        cScope.launch { db.trackDao().delete(item) }
                                    }
                                    true
                                })
                                SwipeToDismiss(dismissState, background = {}) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                            .clickable {
                                                if (showLoading.value) return@clickable
                                                if (playing.value && currItem == item) {
                                                    showPlayer.value = true
                                                    return@clickable
                                                }
                                                showPlayer.value = false
                                                val intent = Intent(context, PlayService::class.java)
                                                intent.putExtra("index", i)
                                                service?.resetMP()
                                                activity?.bindService(intent, this@MyMusicFragment, BIND_AUTO_CREATE)
                                                activity?.startService(intent)
                                                showLoading.value = true
                                            }) {
                                        val btp = remember { mutableStateOf<Bitmap?>(null) }
                                        val url = item.img
                                        setBitmap(btp, url)
                                        val bitmap = btp.value?.asImageBitmap()
                                        if (bitmap != null) Image(bitmap, null, modifier = Modifier.height(90.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text(item.name, fontSize = 21.sp)
                                            Text(item.source)
                                        }
                                    }
                                }
                            }
                        }
                        if (showPlayer.value) Card(
                            Modifier
                                .constrainAs(createRef()) {
                                    bottom.linkTo(parent.bottom, margin = 4.dp)
                                }
                                .padding(16.dp), backgroundColor = lightBlue, elevation = 4.dp) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = CenterHorizontally) {
                                if (adLoaded.value) {
                                    val icon = MediaView(requireContext())
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                    ) {
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
                                        AndroidView({ AdOptionsView(it, nativeAd, null) })
                                    }
                                    AndroidView(
                                        {
                                            val mediaView = MediaView(it)
                                            nativeAd.registerViewForInteraction(mediaView, mediaView, icon)
                                            mediaView
                                        },
                                        Modifier
                                            .background(Color.White)
                                            .padding(bottom = 8.dp)
                                            .height(251.dp)
                                    )
                                }
                                Text(currItem.name, color = Color.White, fontSize = 17.sp)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                                    val modifier = Modifier
                                        .padding(start = 8.dp, end = 8.dp)
                                        .width(48.dp)
                                    Button(
                                        {
                                            if (playing.value) {
                                                service?.pause()
                                                playing.value = false
                                            } else {
                                                service?.play()
                                                playing.value = true
                                            }
                                        }, modifier
                                    ) {
                                        Icon(
                                            painterResource(id = if (playing.value) R.drawable.ic_pause_24 else R.drawable.ic_play_24),
                                            getString(R.string.play), tint = Color.White
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = progress.value,
                                        Modifier
                                            .height(7.dp)
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
                                                    service?.seekTo(progress.value)
                                                }
                                            }, colorPrimary
                                    )
                                    Button(onClick = { showPlayer.value = false }, modifier) {
                                        Icon(
                                            painterResource(R.drawable.remove), getString(R.string.delete),
                                            Modifier.scale(4F), Color.White
                                        )
                                    }
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "on stop")
        activity?.unbindService(this)
    }

    private fun setBitmap(btp: MutableState<Bitmap?>, url: String?) {
        val bitmap = imageCache[url]
        if (bitmap != null) btp.value = bitmap
        else cScope.launch {
            val v = try {
                if (url == null) throw IOException();
                BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
            } catch (ce: IOException) {
                if (!isDetached) BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                else null
            }
            withContext(Dispatchers.Main) {
                btp.value = v
            }
            imageCache[url] = btp.value
        }

    }

    fun update() = cScope.launch {
        val elements = db.trackDao().all()
        withContext(Dispatchers.Main) {
            Log.i(TAG, "tracks from db=>$elements")
            tracks.clear()
            tracks.addAll(elements)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this.service = service as PlayService.PlayBinder
        service.listener = this
        Log.i(TAG, "on service connected")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "onServiceDisconnected")
        service = null
    }

    override fun showProgress(b: Boolean) {
        showLoading.value = b
        showPlayer.value = !b
        playing.value = !b
    }

    override fun onProgress(progress: Float) {
        this.progress.value = progress
    }

    override fun setCurrItem(t: PlayTrack) {
        currItem = t
        Log.i(TAG, "setCurrItem=>$t")
    }

    override fun setPlaying(playing: Boolean) {
        this.playing.value = playing
        if (!showPlayer.value) showPlayer.value = true
    }

    override fun showPlayer(b: Boolean) {
        showPlayer.value = b
    }
}



