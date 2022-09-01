package z.zer.tor.media.android.db

import androidx.annotation.Keep
import androidx.room.*


@Database(entities = [PlayTrack::class], version = 3, exportSchema = false)
abstract class Db : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    @Keep
    @Dao
    interface TrackDao {

        @Insert
        suspend fun insert(track: PlayTrack)

        @Query("SELECT * FROM PlayTrack")
        suspend fun all(): List<PlayTrack>

        @Delete
        suspend fun delete(track: PlayTrack)

        @Query("SELECT EXISTS(SELECT 1 FROM PlayTrack WHERE id=:i)")
        suspend fun isAdded(i: String): Boolean

        @Query("DELETE FROM PlayTrack WHERE id=:id")
        suspend fun deleteById(id: String)
    }
}