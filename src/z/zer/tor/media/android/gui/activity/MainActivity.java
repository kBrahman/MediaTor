package z.zer.tor.media.android.gui.activity;

import static com.andrew.apollo.utils.MusicUtils.musicPlaybackService;
import static z.zer.tor.media.android.util.Asyncs.async;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Stack;

import z.zer.tor.media.R;
import z.zer.tor.media.android.AndroidPlatform;
import z.zer.tor.media.android.StoragePicker;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.LocalSearchEngine;
import z.zer.tor.media.android.gui.NetworkManager;
import z.zer.tor.media.android.gui.activity.internal.MainController;
import z.zer.tor.media.android.gui.dialogs.HandpickedTorrentDownloadDialogOnFetch;
import z.zer.tor.media.android.gui.dialogs.NewTransferDialog;
import z.zer.tor.media.android.gui.dialogs.SDPermissionDialog;
import z.zer.tor.media.android.gui.dialogs.YesNoDialog;
import z.zer.tor.media.android.gui.fragments.MainFragment;
import z.zer.tor.media.android.gui.fragments.MyFilesFragment;
import z.zer.tor.media.android.gui.fragments.SearchFragment;
import z.zer.tor.media.android.gui.fragments.TransfersFragment;
import z.zer.tor.media.android.gui.fragments.TransfersFragment.TransferStatus;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.android.gui.transfers.TransferManager;
import z.zer.tor.media.android.gui.util.DangerousPermissionsChecker;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.gui.views.AbstractDialog.OnDialogClickListener;
import z.zer.tor.media.android.gui.views.MiniPlayerView;
import z.zer.tor.media.android.gui.views.TimerService;
import z.zer.tor.media.android.gui.views.TimerSubscription;
import z.zer.tor.media.platform.Platforms;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;
import z.zer.tor.media.util.Utils;
import z.zer.tor.media.uxstats.UXAction;
import z.zer.tor.media.uxstats.UXStats;


public class MainActivity extends AbstractActivity implements OnDialogClickListener, ServiceConnection, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final Logger LOG = Logger.getLogger(MainActivity.class);
    private static final String FRAGMENTS_STACK_KEY = "fragments_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static boolean firstTime = true;
    private boolean externalStoragePermissionsRequested = false;

    private final SparseArray<DangerousPermissionsChecker> permissionsCheckers;
    private final Stack<Integer> fragmentsStack;
    private final MainController controller;
    private ServiceToken mToken;
    private Fragment currentFragment;
    private SearchFragment search;
    private MyFilesFragment library;
    private TransfersFragment transfers;
    private final LocalBroadcastReceiver localBroadcastReceiver;
    private TimerSubscription playerSubscription;

    private boolean shuttingDown = false;
    public AdView banner;

    public MainActivity() {
        super(R.layout.activity_main);
        controller = new MainController(this);
        fragmentsStack = new Stack<>();
        permissionsCheckers = initPermissionsCheckers();
        localBroadcastReceiver = new LocalBroadcastReceiver();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!(getCurrentFragment() instanceof SearchFragment)) {
                controller.switchFragment(R.id.menu_main_search);
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleDrawer();
        } else {
            try {
                return super.onKeyDown(keyCode, event);
            } catch (NullPointerException npe) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (fragmentsStack.size() > 1) {
            try {
                fragmentsStack.pop();
                int id = fragmentsStack.peek();
                Fragment fragment = getFragmentManager().findFragmentById(id);
                switchContent(fragment, false);
            } catch (Throwable e) {
                e.printStackTrace();
                super.onBackPressed();
            }
        }
        syncNavigationMenu();
        updateHeader(getCurrentFragment());
        super.onBackPressed();
    }

    public void shutdown() {
        if (shuttingDown) {
            // NOTE: the actual solution should be for a re-architecture for
            // a guarantee of a single call of this logic.
            // For now, just mitigate the double call if coming from the exit
            // and at the same time the close of the interstitial
            return;
        }
        shuttingDown = true;
        LocalSearchEngine.instance().cancelSearch();
        //UXStats.instance().flush(true); // sends data and ends 3rd party APIs sessions.
        finish();
        Engine.instance().shutdown();
        MusicUtils.requestMusicPlaybackServiceShutdown(this);
    }

    @Override
    public void finish() {
        finishAndRemoveTaskViaReflection();
    }

    private void finishAndRemoveTaskViaReflection() {
        final Class<? extends MainActivity> clazz = getClass();
        try {
            final Method finishAndRemoveTaskMethod = clazz.getMethod("finishAndRemoveTask");
            finishAndRemoveTaskMethod.invoke(this);
        } catch (Throwable e) {
            e.printStackTrace();
            super.finish();
        }
    }

    private boolean isShutdown() {
        return isShutdown(null);
    }

    private boolean isShutdown(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        boolean result = intent != null && intent.getBooleanExtra("shutdown-frostwire", false);
        if (result) {
            shutdown();
        }
        return result;
    }

    private boolean isGoHome(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        return intent != null && intent.getBooleanExtra("gohome-frostwire", false);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        if (isShutdown()) {
            return;
        }
        updateNavigationMenu();
        setupFragments();
        setupInitialFragment(savedInstanceState);
        playerSubscription = TimerService.subscribe(((MiniPlayerView) findView(R.id.activity_main_player_notifier)).getRefresher(), 1);
        onNewIntent(getIntent());
        setupActionBar();
    }

    public void updateNavigationMenu(boolean updateAvailable) {
        LOG.info("updateNavigationMenu(" + updateAvailable + ")");
        if (updateAvailable) {
            // make sure it will remember this, even if the menu gets destroyed
            getIntent().putExtra("updateAvailable", true);
        }
    }

    private void updateNavigationMenu() {
        Intent intent = getIntent();
        if (intent != null) {
            updateNavigationMenu(intent.getBooleanExtra("updateAvailable", false));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null || isShutdown(intent)) {
            return;
        }
        if (isGoHome(intent)) {
            finish();
            return;
        }
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Constants.ACTION_SHOW_TRANSFERS:
                    intent.setAction(null);
                    controller.showTransfers(TransferStatus.ALL);
                    break;
                case Intent.ACTION_VIEW:
                    openTorrentUrl(intent);
                    break;
                case Constants.ACTION_START_TRANSFER_FROM_PREVIEW:
                    if (Ref.alive(NewTransferDialog.srRef)) {
                        SearchFragment.startDownload(this, NewTransferDialog.srRef.get(), getString(R.string.download_added_to_queue));
                        UXStats.instance().log(UXAction.DOWNLOAD_CLOUD_FILE_FROM_PREVIEW);
                    }
                    break;
                case Constants.ACTION_REQUEST_SHUTDOWN:
                    UXStats.instance().log(UXAction.MISC_NOTIFICATION_EXIT);
                    showShutdownDialog();
                    break;
            }
        }
        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            async(this, MainActivity::onDownloadCompleteNotification, intent);
        }
        if (intent.hasExtra(Constants.EXTRA_FINISH_MAIN_ACTIVITY)) {
            finish();
        }
    }

    private void openTorrentUrl(Intent intent) {
        try {
            //Open a Torrent from a URL or from a local file :), say from Astro File Manager.
            //Show me the transfer tab
            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            //go!
            final String uri = intent.getDataString();
            intent.setAction(null);
            if (uri != null) {
                if (uri.startsWith("file") || uri.startsWith("http") || uri.startsWith("https") || uri.startsWith("magnet")) {
                    TransferManager.instance().downloadTorrent(uri, new HandpickedTorrentDownloadDialogOnFetch(this));
                } else if (uri.startsWith("content")) {
                    String newUri = saveViewContent(this, Uri.parse(uri), "content-intent.torrent");
                    if (newUri != null) {
                        TransferManager.instance().downloadTorrent(newUri, new HandpickedTorrentDownloadDialogOnFetch(this));
                    }
                }
            } else {
                LOG.warn("MainActivity.onNewIntent(): Couldn't start torrent download from Intent's URI, intent.getDataString() -> null");
                LOG.warn("(maybe URI is coming in another property of the intent object - #fragmentation)");
            }
        } catch (Throwable e) {
            LOG.error("Error opening torrent from intent", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastReceiver.register(this);
        ConfigurationManager CM = ConfigurationManager.instance();
        if (CM.getBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE)) {
            mainResume();
        } else if (!isShutdown()) {
            controller.startWizardActivity();
        }
        checkLastSeenVersionBuild();
        syncNavigationMenu();
        updateNavigationMenu();
        //uncomment to test social links dialog
        //UIUtils.showSocialLinksDialog(this, true, null, "");
        if (CM.getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            checkExternalStoragePermissionsOrBindMusicService();
        }
        async(NetworkManager.instance(), NetworkManager::queryNetworkStatusBackground);
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastReceiver.unregister(this);
    }

    private SparseArray<DangerousPermissionsChecker> initPermissionsCheckers() {
        SparseArray<DangerousPermissionsChecker> checkers = new SparseArray<>();
        // EXTERNAL STORAGE ACCESS CHECKER.
        final DangerousPermissionsChecker externalStorageChecker = new DangerousPermissionsChecker(this, DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        //externalStorageChecker.setPermissionsGrantedCallback(() -> {});
        checkers.put(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE, externalStorageChecker);
        // COARSE
        final DangerousPermissionsChecker accessCoarseLocationChecker = new DangerousPermissionsChecker(this, DangerousPermissionsChecker.ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE);
        checkers.put(DangerousPermissionsChecker.ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE, accessCoarseLocationChecker);
        // add more permissions checkers if needed...
        return checkers;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            // MIGHT DO: save checkedNavViewMenuItemId in bundle.
            outState.putBoolean("updateAvailable", getIntent().getBooleanExtra("updateAvailable", false));
            super.onSaveInstanceState(outState);
            saveLastFragment(outState);
            saveFragmentsStack(outState);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MediaTor);
        super.onCreate(savedInstanceState);
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            return;
        }
        if (isShutdown()) {
            return;
        }
        checkExternalStoragePermissionsOrBindMusicService();
        checkAccessCoarseLocationPermissions();
        banner = new AdView(this, getString(R.string.fb_banner_id), AdSize.BANNER_HEIGHT_50);
        LinearLayout adContainer = findViewById(R.id.banner_container);
        adContainer.addView(banner);
        banner.setVisibility(View.GONE);
        banner.loadAd();
    }

    private void checkAccessCoarseLocationPermissions() {
        DangerousPermissionsChecker checker = permissionsCheckers.get(DangerousPermissionsChecker.ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE);
        if (checker != null && !checker.hasAskedBefore()) {
            checker.requestPermissions();
            ConfigurationManager.instance().setBoolean(Constants.ASKED_FOR_ACCESS_COARSE_LOCATION_PERMISSIONS, true);
        }
    }

    private void checkExternalStoragePermissionsOrBindMusicService() {
        DangerousPermissionsChecker checker = permissionsCheckers.get(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        if (!externalStoragePermissionsRequested && checker != null && checker.noAccess()) {
            checker.requestPermissions();
            externalStoragePermissionsRequested = true;
        } else if (mToken == null && checker != null && !checker.noAccess()) {
            mToken = MusicUtils.bindToService(this, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerSubscription != null) {
            playerSubscription.unsubscribe();
        }
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
        if (banner != null) {
            banner.destroy();
        }
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

//    @Override
//    protected void onStart() {
//        Engine.instance().onApplicationCreate(getApplication());
//        super.onStart();
//    }

    private void mainResume() {
        Log.i(TAG, "main resume");
        async(this, MainActivity::checkSDPermission, MainActivity::checkSDPermissionPost);
        syncNavigationMenu();
        if (firstTime) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_BITTORRENT_ON_VPN_ONLY) && !NetworkManager.instance().isTunnelUp()) {
                UIUtils.showDismissableMessage(findView(R.id.activity_main_drawer_layout), R.string.cannot_start_engine_without_vpn);
            } else {
                firstTime = false;
                Engine.instance().startServices(getApplication()); // it's necessary for the first time after wizard
                Log.i(TAG, "Engine.instance().startServices()");
            }
        }
        if (Engine.instance().wasShutdown()) {
            Engine.instance().startServices(getApplication());
        }
    }

    private void handleSDPermissionDialogClick(int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            StoragePicker.show(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == StoragePicker.SELECT_FOLDER_REQUEST_CODE) {
            StoragePicker.handle(this, requestCode, resultCode, data);
        }
        if (!DangerousPermissionsChecker.handleOnWriteSettingsActivityResult(this)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkLastSeenVersionBuild() {
        final ConfigurationManager CM = ConfigurationManager.instance();
        final String lastSeenVersionBuild = CM.getString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION_BUILD);
        final String currentVersionBuild = Constants.FROSTWIRE_VERSION_STRING + "." + Constants.MEDIAT_TOR_BUILD;
        if (Utils.isNullOrEmpty(lastSeenVersionBuild)) {
            //fresh install
            //Offers.forceDisabledAds(this); // no ads on first session ever
            CM.setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION_BUILD, currentVersionBuild);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_FIRST_TIME);
        } else if (!currentVersionBuild.equals(lastSeenVersionBuild)) {
            //just updated.
            //Offers.forceDisabledAds(this); // no ads right after update
            CM.setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION_BUILD, currentVersionBuild);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_AFTER_UPDATE);
        }
    }

    private void toggleDrawer() {
        syncNavigationMenu();
        updateHeader(getCurrentFragment());
    }

    public void showShutdownDialog() {
        UXStats.instance().flush();
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message, YesNoDialog.FLAG_DISMISS_ON_OK_BEFORE_PERFORM_DIALOG_CLICK);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    public void onDialogClick(String tag, int which) {
        if (tag.equals(SHUTDOWN_DIALOG_ID) && which == Dialog.BUTTON_POSITIVE) {
            onShutdownDialogButtonPositive();
        } else if (tag.equals(SDPermissionDialog.TAG)) {
            handleSDPermissionDialogClick(which);
        }
    }

    private void onShutdownDialogButtonPositive() {
        shutdown();
    }

    public void syncNavigationMenu() {
        invalidateOptionsMenu();
    }

    private void setupFragments() {
        search = (SearchFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_search);
        search.connectDrawerLayoutFilterView(findView(R.id.activity_main_drawer_layout), findView(R.id.activity_main_keyword_filter_drawer_view));
        library = (MyFilesFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_my_files);
        transfers = (TransfersFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_transfers);
    }

    private void hideFragments() {
        try {
            getFragmentManager().executePendingTransactions();
        } catch (Throwable t) {
            LOG.warn(t.getMessage(), t);
        }
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.hide(search).hide(library).hide(transfers);
        try {
            tx.commit();
        } catch (IllegalStateException e) {
            // if not that we can do a lot here, since the root of the problem
            // is the multiple entry points to MainActivity, just let it run
            // a possible inconsistent (but probably right) version.
            // in the future with a higher API, commitNow should be considered
            LOG.warn("Error running commit in fragment transaction, using weaker option", e);
            try {
                tx.commitAllowingStateLoss();
            } catch (IllegalStateException e2) {
                // ¯\_(ツ)_/¯
                LOG.warn("Error running commit in fragment transaction, weaker option also failed (commit already called - mCommited=true)", e2);
            }
        }
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;
        if (savedInstanceState != null) {
            fragment = getFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
            restoreFragmentsStack(savedInstanceState);
        }
        if (fragment == null) {
            fragment = search;
        }
        switchContent(fragment);
    }

    private void saveFragmentsStack(Bundle outState) {
        int[] stack = new int[fragmentsStack.size()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = fragmentsStack.get(i);
        }
        outState.putIntArray(FRAGMENTS_STACK_KEY, stack);
    }

    private void restoreFragmentsStack(Bundle savedInstanceState) {
        try {
            int[] stack = savedInstanceState.getIntArray(FRAGMENTS_STACK_KEY);
            if (stack != null) {
                for (int id : stack) {
                    fragmentsStack.push(id);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateHeader(Fragment fragment) {
        try {
            Toolbar toolbar = findToolbar();
            if (toolbar == null) {
                LOG.warn("updateHeader(): Check your logic, no actionBar available");
                return;
            }
            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (header != null) {
                    setToolbarView(header);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    private void switchContent(Fragment fragment, boolean addToStack) {
        hideFragments();
        FragmentTransaction transaction = getFragmentManager().beginTransaction().show(fragment);
        try {
            transaction.commitAllowingStateLoss();
        } catch (Throwable ignored) {
        }
        if (addToStack && (fragmentsStack.isEmpty() || fragmentsStack.peek() != fragment.getId())) {
            fragmentsStack.push(fragment.getId());
        }
        currentFragment = fragment;
        updateHeader(fragment);
        if (currentFragment instanceof MainFragment) {
            ((MainFragment) currentFragment).onShow();
        }
    }

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByNavMenuId(int id) {
        switch (id) {
            case R.id.menu_main_search:
                return search;
            case R.id.menu_main_library:
                return library;
            case R.id.menu_main_transfers:
                return transfers;
            default:
                return null;
        }
    }

    private int getNavMenuIdByFragment(Fragment fragment) {
        int menuId = -1;
        if (fragment == search) {
            menuId = R.id.menu_main_search;
        } else if (fragment == library) {
            menuId = R.id.menu_main_library;
        } else if (fragment == transfers) {
            menuId = R.id.menu_main_transfers;
        }
        return menuId;
    }

    public void switchContent(Fragment fragment) {
        switchContent(fragment, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item == null) {
            return false;
        }
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setCustomView(R.layout.view_custom_actionbar);
            bar.setDisplayShowCustomEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        musicPlaybackService = IApolloService.Stub.asInterface(service);
    }

    public void onServiceDisconnected(final ComponentName name) {
        musicPlaybackService = null;
    }

    //@Override commented override since we are in API 16, but it will in API 23
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        DangerousPermissionsChecker checker = permissionsCheckers.get(requestCode);
        if (checker != null) {
            checker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void performYTSearch(String ytUrl) {
        SearchFragment searchFragment = (SearchFragment) getFragmentByNavMenuId(R.id.menu_main_search);
        searchFragment.performYTSearch(ytUrl);
        switchContent(searchFragment);
    }

    public static void refreshTransfers(Context context) {
        Intent intent = new Intent(context, MainActivity.class).
                setAction(Constants.ACTION_SHOW_TRANSFERS).
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(intent);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    private void onDownloadCompleteNotification(Intent intent) {
        controller.showTransfers(TransferStatus.COMPLETED);
        TransferManager.instance().clearDownloadsToReview();
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED);
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String downloadCompletePath = extras.getString(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH);
                if (downloadCompletePath != null) {
                    File file = new File(downloadCompletePath);
                    if (file.isFile()) {
                        UIUtils.openFile(this, file.getAbsoluteFile());
                    }
                }
            }
        } catch (Throwable e) {
            LOG.warn("Error handling download complete notification", e);
        }
    }

    /**
     * @return true if the SD Permission dialog must be shown
     */
    private boolean checkSDPermission() {
        if (!AndroidPlatform.saf()) {
            return false;
        }
        try {
            File data = Platforms.data();
            File parent = data.getParentFile();
            return AndroidPlatform.saf(parent) && (!Platforms.fileSystem().canWrite(parent) && !SDPermissionDialog.visible);
        } catch (Throwable e) {
            // we can't do anything about this
            LOG.error("Unable to detect if we have SD permissions", e);
            return false;
        }
    }

    private void checkSDPermissionPost(boolean showPermissionDialog) {
        if (showPermissionDialog) {
            SDPermissionDialog dlg = SDPermissionDialog.newInstance();
            FragmentManager fragmentManager = getFragmentManager();
            try {
                if (fragmentManager != null) {
                    dlg.show(fragmentManager);
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    // TODO: refactor and move this method for a common place when needed
    private static String saveViewContent(Context context, Uri uri, String name) {
        InputStream inStream = null;
        OutputStream outStream = null;
        if (!Platforms.temp().exists()) {
            boolean mkdirs = Platforms.temp().mkdirs();
            if (!mkdirs) {
                LOG.warn("saveViewContent() could not create Platforms.temp() directory.");
            }
        }
        File target = new File(Platforms.temp(), name);
        try {
            inStream = context.getContentResolver().openInputStream(uri);
            outStream = new FileOutputStream(target);
            byte[] buffer = new byte[16384]; // MAGIC_NUMBER
            int bytesRead;
            if (inStream != null) {
                while ((bytesRead = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error when copying file from " + uri + " to temp/" + name, e);
            return null;
        } finally {
            IOUtils.closeQuietly(inStream);
            IOUtils.closeQuietly(outStream);
        }
        return "file://" + target.getAbsolutePath();
    }

    private final class LocalBroadcastReceiver extends BroadcastReceiver {

        private final IntentFilter intentFilter;

        LocalBroadcastReceiver() {
            intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_NOTIFY_UPDATE_AVAILABLE);
            intentFilter.addAction(Constants.ACTION_NOTIFY_DATA_INTERNET_CONNECTION);
        }

        public void register(Context context) {
            LocalBroadcastManager.getInstance(context).registerReceiver(this, intentFilter);
        }

        public void unregister(Context context) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constants.ACTION_NOTIFY_UPDATE_AVAILABLE.equals(action)) {
                boolean value = intent.getBooleanExtra("value", false);
                Intent mainActivityIntent = getIntent();
                if (mainActivityIntent != null) {
                    mainActivityIntent.putExtra("updateAvailable", value);
                }
                updateNavigationMenu(value);
            }
            if (Constants.ACTION_NOTIFY_DATA_INTERNET_CONNECTION.equals(action)) {
                boolean isDataUp = intent.getBooleanExtra("isDataUp", true);
                if (!isDataUp) {
                    UIUtils.showDismissableMessage(findView(android.R.id.content), R.string.no_data_check_internet_connection);
                }
            }
        }
    }
}
