package z.music.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Track(@PrimaryKey val id: String, val name: String, val source: String, val url: String, val img: String)