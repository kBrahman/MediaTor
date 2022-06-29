package z.zer.tor.media.android.gui.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class MenuBuilder implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private final MenuAdapter adapter;

    private AlertDialog dialog;

    public MenuBuilder(MenuAdapter adapter) {
        this.adapter = adapter;
    }

    public AlertDialog show() {
        createDialog().show();
        return dialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        MenuAction item = adapter.getItem(which);
        item.onClick();
        cleanup();
    }

    public void onCancel(DialogInterface dialog) {
        cleanup();
    }

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext());

        //builder.setTitle(adapter.getTabTitle());
        builder.setAdapter(adapter, this);
        builder.setInverseBackgroundForced(true);

        dialog = builder.create();
        dialog.setOnCancelListener(this);
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    private void cleanup() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
