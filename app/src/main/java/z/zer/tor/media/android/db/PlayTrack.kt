package z.zer.tor.media.android.db

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class PlayTrack(@PrimaryKey val id: String, val name: String, val source: String, val url: String, val img: String?)