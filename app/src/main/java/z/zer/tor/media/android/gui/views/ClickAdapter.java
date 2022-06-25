package z.zer.tor.media.android.gui.views;

import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;

import java.lang.ref.WeakReference;

import z.zer.tor.media.util.Ref;

public abstract class ClickAdapter<T> implements View.OnClickListener, View.OnLongClickListener, View.OnKeyListener, DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = ClickAdapter.class.getSimpleName();
    protected final WeakReference<T> ownerRef;

    public ClickAdapter(T owner) {
        this.ownerRef = Ref.weak(owner);
    }

    @Override
    public final void onClick(View v) {
        Log.i(TAG, "on click");
        if (Ref.alive(ownerRef)) {
            onClick(ownerRef.get(), v);
        }
    }

    @Override
    public final boolean onLongClick(View v) {
        return Ref.alive(ownerRef) && onLongClick(ownerRef.get(), v);
    }

    @Override
    public final boolean onKey(View v, int keyCode, KeyEvent event) {
        return Ref.alive(ownerRef) && onKey(ownerRef.get(), v, keyCode, event);
    }

    @Override
    public final void onClick(DialogInterface dialog, int which) {
        if (Ref.alive(ownerRef)) {
            onClick(ownerRef.get(), dialog, which);
        }
    }

    @Override
    public final void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (Ref.alive(ownerRef)) {
            onCheckedChanged(ownerRef.get(), buttonView, isChecked);
        }
    }

    public void onClick(T owner, View v) {
    }

    @SuppressWarnings("unused")
    public boolean onLongClick(T owner, View v) {
        return false;
    }

    public boolean onKey(T owner, View v, int keyCode, KeyEvent event) {
        return false;
    }

    @SuppressWarnings("unused")
    public void onClick(T owner, DialogInterface dialog, int which) {
    }

    public void onCheckedChanged(T owner, CompoundButton buttonView, boolean isChecked) {
    }
}
