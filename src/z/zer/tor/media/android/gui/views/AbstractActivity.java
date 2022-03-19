package z.zer.tor.media.android.gui.views;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import z.zer.tor.media.R;
import z.zer.tor.media.android.util.Debug;

public abstract class AbstractActivity extends FragmentActivity {

    private static final String TAG = "AbstractActivity";
    private final int layoutResId;
    private final ArrayList<String> fragmentTags;

    private boolean paused;

    public AbstractActivity(@LayoutRes int layoutResId) {
        this.layoutResId = layoutResId;
        this.fragmentTags = new ArrayList<>();
        this.paused = false;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        String tag = fragment.getTag();
        if (tag != null && !fragmentTags.contains(tag)) {
            fragmentTags.add(tag);
        }
    }

    @Override
    protected void onResume() {
        paused = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        paused = true;
        super.onPause();
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Returns a list of the currently attached fragments with
     * a non null TAG.
     * <p>
     * If you are in API >= 26, the new method {@link FragmentManager#getFragments()}
     * give you access to a list of all fragments that are added to the FragmentManager.
     *
     * @return the list of attached fragments with TAG.
     */
    public final List<Fragment> getFragments() {
        List<Fragment> result = new LinkedList<>();

        FragmentManager fm = getFragmentManager();
        for (String tag : fragmentTags) {
            Fragment f = fm.findFragmentByTag(tag);
            if (f != null) {
                result.add(f);
            }
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutResId);
        initComponents(savedInstanceState);
        Log.i(TAG,"onCreate");
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        Toolbar toolbar = findToolbar();
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean r = super.onCreateOptionsMenu(menu);
        return r;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (Throwable e) {
            if (Debug.isEnabled()) {
                // rethrow to actually see it and fix it
                throw e;
            }
            // else, ignore exception, it could be to a bad call from
            // third party frameworks
        }
    }

    protected abstract void initComponents(Bundle savedInstanceState);

    protected final <T extends View> T findView(@IdRes int id) {
        return super.findViewById(id);
    }

    /**
     * Returns the first fragment of type T (and with a non null tag) found in the
     * internal fragment manager.
     * <p>
     * This method should cover 98% of the actual UI designs, if you ever get to have
     * two fragments of the same type in the activity, review the components design.
     * If you present two different sets of information with the same type, that's not
     * a good OOP design, and if you present the same information, that's not a good
     * UI/UX design.
     *
     * @param clazz the class of the fragment to lookup
     * @param <T>   the type of the fragment to lookup
     * @return the first fragment of type T if found.
     */
    @SuppressWarnings("unchecked")
    public final <T extends Fragment> T findFragment(Class<T> clazz) {
        for (Fragment f : getFragments()) {
            if (clazz.isInstance(f)) {
                return (T) f;
            }
        }
        return null;
    }

    public final Toolbar findToolbar() {
        return findView(R.id.toolbar_main);
    }

    /**
     * This settings is application wide and apply to all activities and
     * fragments that use our internal abstract activity. This enable
     * or disable the menu icons for both options and context menu.
     *
     */
    public static void setMenuIconsVisible() {
    }

}
