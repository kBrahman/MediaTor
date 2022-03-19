package z.zer.tor.media.android.gui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
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
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import z.music.db.Track
import z.zer.tor.media.R
import z.zer.tor.media.android.db.Db
import z.zer.tor.media.android.gui.activity.PlayerActivity
import java.io.IOException
import java.net.URL

class MyMusicFragment : Fragment() {
    companion object {
        private const val TAG = "MyMusicFragment"
    }

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
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        TopAppBar {
                            Text(getString(R.string.my_music))
                        }
                        LazyColumn(contentPadding = PaddingValues(4.dp)) {
                            items(tracks.size) {
                                val item = tracks[it]
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                        .clickable {
                                            val i = Intent(context, PlayerActivity::class.java)
                                            i.putExtra("displayName", item.name)
                                            i.putExtra("source", item.source)
                                            i.putExtra("streamUrl", item.url)
                                            i.putExtra("image_url", item.img)
                                            startActivity(i)
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
                BitmapFactory.decodeResource(resources, R.drawable.contextmenu_icon_play)
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

}

