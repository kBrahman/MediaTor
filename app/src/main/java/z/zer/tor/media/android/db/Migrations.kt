package z.zer.tor.media.android.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Track RENAME TO PlayTrack")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE tmp_PLayTrack(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, source TEXT NOT NULL," +
                        "url TEXT NOT NULL, img TEXT)"
            )
            database.execSQL("INSERT INTO tmp_PlayTrack SELECT * FROM PlayTrack")
            database.execSQL("DROP TABLE PlayTrack")
            database.execSQL("ALTER TABLE tmp_PlayTrack RENAME TO PlayTrack")
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction()
        }
    }
}
