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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import z.zer.tor.media.R
import z.zer.tor.media.android.db.Db
import z.zer.tor.media.android.db.Track
import z.zer.tor.media.android.service.PlayService
import java.io.IOException
import java.net.URL

@ExperimentalMaterialApi
class MyMusicFragment : Fragment(), ServiceConnection {
    companion object {
        private const val TAG = "MyMusicFragment"
    }

    private lateinit var service: PlayService.PlayBinder
    private lateinit var tracks: SnapshotStateList<Track>
    private lateinit var db: Db
    private val cScope = CoroutineScope(Dispatchers.IO)
    private val imageCache = HashMap<String, Bitmap?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(
            requireContext(),
            Db::class.java,
            getString(R.string.application_label) + "_db"
        ).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(TAG, "onCreateView")
        return ComposeView(requireContext()).apply {
            val colorPrimary = Color(getColor(context, R.color.colorPrimary))
            setContent {
                MaterialTheme(colors = lightColors(colorPrimary)) {
                    tracks = remember<SnapshotStateList<Track>> { mutableStateListOf() }.apply { update() }
                    ConstraintLayout(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        val (bar, content) = createRefs()
                        val showPlayer = remember { mutableStateOf(false) }
                        val playing = remember { mutableStateOf(false) }
                        val showLoading = remember { mutableStateOf(false) }
                        TopAppBar(Modifier.constrainAs(bar) {}) {
                            Text(getString(R.string.my_music))
                        }
                        if (showLoading.value) LinearProgressIndicator(
                            Modifier
                                .fillMaxWidth()
                                .constrainAs(createRef()) {
                                    top.linkTo(bar.bottom)
                                }, Color(R.color.basic_blue_highlight), Color.White
                        )
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
                                                val intent = Intent(context, PlayService::class.java)
                                                intent.putExtra("index", i)
                                                startForegroundService(context, intent)
//                                                activity?.bindService(intent, this@MyMusicFragment, BIND_AUTO_CREATE)
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
                        if (showPlayer.value) Card(Modifier.constrainAs(createRef()) {
                            bottom.linkTo(parent.bottom, margin = 4.dp)
                        }, elevation = 4.dp) {
                            if (playing.value)
                                Row() {

                                }

                        }

                    }
                }
            }
        }
    }

    private fun setBitmap(btp: MutableState<Bitmap?>, url: String) {
        val bitmap = imageCache[url]
        if (bitmap != null) btp.value = bitmap
        else cScope.launch {
            val v = try {
                BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
            } catch (ce: IOException) {
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
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
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

}



