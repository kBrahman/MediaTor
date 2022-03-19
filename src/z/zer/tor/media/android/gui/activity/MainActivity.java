package z.zer.tor.media.android.gui.activity;

import static z.zer.tor.media.android.util.Asyncs.async;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.Method;
import java.util.Stack;

import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.LocalSearchEngine;
import z.zer.tor.media.android.gui.NetworkManager;
import z.zer.tor.media.android.gui.activity.internal.MainController;
import z.zer.tor.media.android.gui.dialogs.YesNoDialog;
import z.zer.tor.media.android.gui.fragments.MyMusicFragment;
import z.zer.tor.media.android.gui.fragments.SearchFragment;
import z.zer.tor.media.android.gui.util.DangerousPermissionsChecker;
import z.zer.tor.media.android.gui.views.AbstractActivity;
import z.zer.tor.media.android.gui.views.AbstractDialog.OnDialogClickListener;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Utils;
import z.zer.tor.media.uxstats.UXAction;
import z.zer.tor.media.uxstats.UXStats;


public class MainActivity extends AbstractActivity implements OnDialogClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final Logger LOG = Logger.getLogger(MainActivity.class);
    private static final String FRAGMENTS_STACK_KEY = "fragments_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";
    private static final String TAG = MainActivity.class.getSimpleName();

    private final SparseArray<DangerousPermissionsChecker> permissionsCheckers;
    private final Stack<Integer> fragmentsStack;
    private final MainController controller;
    private ServiceToken mToken;
    private Fragment currentFragment;
    private SearchFragment search;

    private boolean shuttingDown = false;
    private TabLayout tabLayout;
    private final SparseArray<FileTypeTab> toFileTypeTab = new SparseArray<>();
    private Fragment myMusicFragment;

    public void selectTabByMediaType(final byte mediaTypeId) {
        FileTypeTab fileTypeTab = toFileTypeTab.get(mediaTypeId);
        if (fileTypeTab != null && tabLayout != null) {
            TabLayout.Tab tab = tabLayout.getTabAt(fileTypeTab.position);
            if (tab != null) {
                tab.select();
            }
        }
    }

    public MainActivity() {
        super(R.layout.activity_main);
        controller = new MainController(this);
        fragmentsStack = new Stack<>();
        permissionsCheckers = initPermissionsCheckers();
        Log.i(TAG, "MainActivity()");
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

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        updateNavigationMenu();
        setupFragments();
        setupInitialFragment(savedInstanceState);
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
    protected void onResume() {
        super.onResume();
        ConfigurationManager CM = ConfigurationManager.instance();
        checkLastSeenVersionBuild();
        syncNavigationMenu();
        updateNavigationMenu();
        async(NetworkManager.instance(), NetworkManager::queryNetworkStatusBackground);
        selectTabByMediaType((byte) CM.getLastMediaTypeFilter());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ((MyMusicFragment) myMusicFragment).update();
        Log.i(TAG, "onRestart");
    }

    private SparseArray<DangerousPermissionsChecker> initPermissionsCheckers() {
        SparseArray<DangerousPermissionsChecker> checkers = new SparseArray<>();
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
        tabLayout = findViewById(R.id.tabs);
        TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.i(TAG, "on tab selected");
                tabItemFileTypeClick(FileTypeTab.at(tab.getPosition()).fileType);
                if (tab.getPosition() == 0) {
                    Log.i(TAG, "selected audio");
                    controller.switchFragment(R.id.menu_main_search);
                } else {
                    Log.i(TAG, "selected my music");
                    controller.switchFragment(R.id.menu_main_my_music);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                tabItemFileTypeClick(FileTypeTab.at(tab.getPosition()).fileType);
            }
        };
        tabLayout.addOnTabSelectedListener(tabSelectedListener);
        Log.i(TAG, "addOnTabSelectedListener");
        toFileTypeTab.put(Constants.FILE_TYPE_AUDIO, FileTypeTab.TAB_AUDIO);
        toFileTypeTab.put(Constants.FILE_TYPE_MY_MUSIC, FileTypeTab.TAB_MY_MUSIC);
        controller.switchFragment(R.id.menu_main_search);
    }

    public enum FileTypeTab {
        TAB_AUDIO(Constants.FILE_TYPE_AUDIO, 0), TAB_MY_MUSIC(Constants.FILE_TYPE_MY_MUSIC, 1);

        final byte fileType;
        public final int position;

        FileTypeTab(byte fileType, int position) {
            this.fileType = fileType;
            this.position = position;
        }

        static FileTypeTab at(int position) {
            return FileTypeTab.values()[position];
        }
    }

    private void tabItemFileTypeClick(final int fileType) {
        onMediaTypeSelected(fileType);
    }

    public void onMediaTypeSelected(int mediaTypeId) {
        Log.i(TAG, "onMediaTypeSelected");
        if (search.adapter.getFileType() != mediaTypeId) {
            ConfigurationManager.instance().setLastMediaTypeFilter(mediaTypeId);
            search.adapter.setFileType(mediaTypeId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
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
    }

    public void showShutdownDialog() {
        UXStats.instance().flush();
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message, YesNoDialog.FLAG_DISMISS_ON_OK_BEFORE_PERFORM_DIALOG_CLICK);
        dlg.show(getSupportFragmentManager()); //see onDialogClick
    }

    public void onDialogClick(String tag, int which) {
        if (tag.equals(SHUTDOWN_DIALOG_ID) && which == Dialog.BUTTON_POSITIVE) {
            onShutdownDialogButtonPositive();
        }
    }

    private void onShutdownDialogButtonPositive() {
        shutdown();
    }

    public void syncNavigationMenu() {
        invalidateOptionsMenu();
    }

    private void setupFragments() {
        myMusicFragment = getSupportFragmentManager().findFragmentById(R.id.activity_main_fragment_my_music);
        search = (SearchFragment) getSupportFragmentManager().findFragmentById(R.id.activity_main_fragment_search);
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
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

    private void switchContent(Fragment fragment, boolean addToStack) {
        hideFragments();
        if (fragment instanceof MyMusicFragment) ((MyMusicFragment) fragment).update();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().show(fragment);
        try {
            transaction.commitAllowingStateLoss();
        } catch (Throwable ignored) {
        }
        if (addToStack && (fragmentsStack.isEmpty() || fragmentsStack.peek() != fragment.getId())) {
            fragmentsStack.push(fragment.getId());
        }
        currentFragment = fragment;
    }

    private void hideFragments() {
        try {
            getSupportFragmentManager().executePendingTransactions();
        } catch (Throwable t) {
            LOG.warn(t.getMessage(), t);
        }
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.hide(search).hide(myMusicFragment);
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

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByNavMenuId(int id) {
        switch (id) {
            case R.id.menu_main_search:
                return search;
            default:
                return myMusicFragment;
        }
    }

    public void switchContent(Fragment fragment) {
        switchContent(fragment, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
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

    //@Override commented override since we are in API 16, but it will in API 23
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        DangerousPermissionsChecker checker = permissionsCheckers.get(requestCode);
        if (checker != null) {
            checker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
