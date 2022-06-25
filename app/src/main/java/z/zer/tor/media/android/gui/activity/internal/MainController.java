package z.zer.tor.media.android.gui.activity.internal;


import android.util.Log;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.util.Ref;


public final class MainController {

    private static final String TAG = "MainController";
    private final WeakReference<MainActivity> activityRef;

    public MainController(MainActivity activity) {
        activityRef = Ref.weak(activity);
    }

    public MainActivity getActivity() {
        if (!Ref.alive(activityRef)) {
            return null;
        }
        return activityRef.get();
    }

    public void switchFragment(int itemId) {
        Log.i(TAG, "switchFragment");
        if (!Ref.alive(activityRef)) {
            return;
        }
        MainActivity activity = activityRef.get();
        Fragment fragment = activity.getFragmentByNavMenuId(itemId);
        if (fragment != null) {
            activity.switchContent(fragment);
        }
        Log.i(TAG, "switchFragment finish");
    }

    public void setTitle(CharSequence title) {
        if (!Ref.alive(activityRef)) {
            return;
        }
        MainActivity activity = activityRef.get();
        activity.setTitle(title);
    }

}
