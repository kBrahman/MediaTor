package z.zer.tor.media.android.gui.views;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;

public abstract class AbstractFragment extends Fragment {

    private final int layoutResId;

    private boolean paused;

    public AbstractFragment(int layoutResId) {
        this.layoutResId = layoutResId;
        this.paused = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(layoutResId, container, false);
        initComponents(rootView, savedInstanceState);
        return rootView;
    }

    @Override
    public void onResume() {
        paused = false;
        super.onResume();
    }

    @Override
    public void onPause() {
        paused = true;
        super.onPause();
    }

    public boolean isPaused() {
        return paused;
    }

    public final void startActionMode(ActionMode.Callback callback) {
        Activity activity = getActivity();

        if (!(activity instanceof AppCompatActivity))
            throw new UnsupportedOperationException("operation only supported when using AppCompatActivity");

        ((AppCompatActivity) activity).startSupportActionMode(callback);
    }

    /**
     * onCreateView calls this before it returns the rootView it has just inflated
     *
     * @param rootView
     * @param savedInstanceState
     */
    protected void initComponents(final View rootView, Bundle savedInstanceState) {
    }

    protected final <T extends View> T findView(View v, @IdRes int id) {
        T result = null;
        if (v != null) {
            result = v.findViewById(id);
        }
        return result;
    }
}
