package z.zer.tor.media.android.gui.activity.internal;

import android.app.Fragment;
import android.content.res.Configuration;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import z.zer.tor.media.R;
import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.android.gui.fragments.TransfersFragment;
import z.zer.tor.media.android.gui.services.Engine;
import z.zer.tor.media.android.gui.util.UIUtils;

public final class NavigationMenu {
    private final MainController controller;
    private final NavigationView navView;
    private final DrawerLayout drawerLayout;
    private final ActionBarDrawerToggle drawerToggle;
    private int checkedNavViewMenuItemId = -1;

    public NavigationMenu(MainController controller, DrawerLayout drawerLayout, Toolbar toolbar) {
        this.controller = controller;
        this.drawerLayout = drawerLayout;
        MainActivity mainActivity = controller.getActivity();
        drawerToggle = new MenuDrawerToggle(controller, drawerLayout, toolbar);
        this.drawerLayout.addDrawerListener(drawerToggle);
        navView = initNavigationView(mainActivity);
    }

    public boolean isOpen() {
        return drawerLayout.isDrawerOpen(navView);
    }

    public void show() {
        drawerLayout.openDrawer(navView);
    }

    public void hide() {
        drawerLayout.closeDrawer(navView);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void syncState() {
        drawerToggle.syncState();
    }

    public void updateCheckedItem(int menuItemId) {
        navView.setCheckedItem(menuItemId);
    }

    private NavigationView initNavigationView(final MainActivity activity) {
        NavigationView resultNavView = navView;
        if (navView == null) {
            resultNavView = activity.findViewById(R.id.activity_main_nav_view);
            resultNavView.setNavigationItemSelectedListener(menuItem -> {
                onMenuItemSelected(menuItem);
                return true;
            });
            View navViewHeader = resultNavView.getHeaderView(0);
            // Logo
            ImageView navLogo = navViewHeader.findViewById(R.id.nav_view_header_main_app_logo);

            // Prep title and version
            TextView title = navViewHeader.findViewById(R.id.nav_view_header_main_title);

            title.setText(title.getContext().getString(R.string.application_label));

            // Prep update button
            ImageView updateButton = navViewHeader.findViewById(R.id.nav_view_header_main_update);
            updateButton.setVisibility(View.GONE);
            updateButton.setOnClickListener(v -> onUpdateButtonClicked());
        }
        return resultNavView;
    }

    private void onMenuItemSelected(MenuItem menuItem) {
        if (controller.getActivity() == null) {
            return;
        }
        checkedNavViewMenuItemId = menuItem.getItemId();
        Engine.instance().hapticFeedback();
        controller.syncNavigationMenu();
        menuItem.setChecked(true);
        controller.setTitle(menuItem.getTitle());
        int menuActionId = menuItem.getItemId();

        Fragment fragment = controller.getFragmentByNavMenuId(menuItem.getItemId());
        if (fragment != null) {
            controller.switchContent(fragment);
        } else {
            switch (menuActionId) {
                case R.id.menu_main_my_music:
                    controller.launchMyMusic();
                    break;
                case R.id.menu_main_library:
                    controller.showMyFiles();
                    break;
                case R.id.menu_main_transfers:
                    controller.showTransfers(TransfersFragment.TransferStatus.ALL);
                    break;
                case R.id.menu_main_settings:
                    controller.showPreferences();
                    break;
                case R.id.menu_main_shutdown:
                    controller.showShutdownDialog();
                    break;
                default:
                    break;
            }
        }

        hide();
    }

    private void onUpdateButtonClicked() {
        hide();
    }

    public void onUpdateAvailable() {
        View navViewHeader = navView.getHeaderView(0);
        ImageView updateButton = navViewHeader.findViewById(R.id.nav_view_header_main_update);
        updateButton.setVisibility(View.VISIBLE);
    }

    public MenuItem getCheckedItem() {
        return navView.getMenu().findItem(checkedNavViewMenuItemId != -1 ? checkedNavViewMenuItemId : R.id.menu_main_search);
    }

    public void onOptionsItemSelected(MenuItem item) {
        drawerToggle.onOptionsItemSelected(item);
    }

    private final class MenuDrawerToggle extends ActionBarDrawerToggle {
        private final MainController controller;

        MenuDrawerToggle(MainController controller, DrawerLayout drawerLayout, Toolbar toolbar) {
            super(controller.getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
            this.controller = controller;
        }

        @Override
        public void onDrawerClosed(View view) {
            controller.syncNavigationMenu();
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (controller.getActivity() != null) {
                UIUtils.hideKeyboardFromActivity(controller.getActivity());
            }
            controller.syncNavigationMenu();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            controller.syncNavigationMenu();
        }
    }
}
