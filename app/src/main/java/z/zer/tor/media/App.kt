package z.zer.tor.media

import android.app.Application
import androidx.room.Room
import com.facebook.ads.AudienceNetworkAds
import z.zer.tor.media.android.core.ConfigurationManager
import z.zer.tor.media.android.db.Db
import z.zer.tor.media.android.db.MIGRATION_1_2
import z.zer.tor.media.android.db.MIGRATION_2_3
import z.zer.tor.media.android.gui.LocalSearchEngine
import z.zer.tor.media.android.gui.NetworkManager
import z.zer.tor.media.android.gui.views.AbstractActivity
import z.zer.tor.media.android.util.Asyncs
import z.zer.tor.media.android.util.ImageLoader
import z.zer.tor.media.android.util.RunStrict

class App : Application() {


    lateinit var db: Db

    override fun onCreate() {
        super.onCreate()
        RunStrict.runStrict { onCreateSafe() }
        ImageLoader.start(this)
        Asyncs.async { LocalSearchEngine.instance() }
        AudienceNetworkAds.initialize(this)
        db = Room.databaseBuilder(
            this,
            Db::class.java,
            getString(R.string.application_label) + "_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    private fun onCreateSafe() {
        ConfigurationManager.create(this)

        // some phones still can configure an external button as the
        // permanent menu key
//        ignoreHardwareMenu();
        AbstractActivity.setMenuIconsVisible()
        NetworkManager.create(this)
        Asyncs.async(NetworkManager.instance()) { manager: NetworkManager? ->
            NetworkManager.queryNetworkStatusBackground(
                manager
            )
        }
    }
}