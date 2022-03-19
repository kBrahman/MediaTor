package z.zer.tor.media.android.db

import androidx.room.*


@Database(entities = [Track::class], version = 1)
abstract class Db : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    @Dao
    interface TrackDao {

        @Insert
        suspend fun insert(track: Track)

        @Query("SELECT * FROM Track")
        suspend fun all(): List<Track>

        @Delete
        suspend fun delete(track: Track)

        @Query("SELECT EXISTS(SELECT 1 FROM Track WHERE id=:i)")
        suspend fun isAdded(i: String): Boolean

        @Query("DELETE FROM Track WHERE id=:id")
        suspend fun deleteById(id: String)
    }
}