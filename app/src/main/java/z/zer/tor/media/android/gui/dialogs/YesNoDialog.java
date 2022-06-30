package z.zer.tor.media.android.gui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import z.zer.tor.media.R;
import z.zer.tor.media.android.gui.views.AbstractDialog;

public class YesNoDialog extends AbstractDialog {
    public static final byte FLAG_DISMISS_ON_OK_BEFORE_PERFORM_DIALOG_CLICK = 0x1;
    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String MESSAGE_ID_KEY = "messageId";
    private static final String MESSAGE_STRING_KEY = "messageStr";
    private static final String YES_NO_DIALOG_FLAGS = "yesnodialog_flags";
    private String id;

    public YesNoDialog() {
        super(R.layout.dialog_default);
    }

    public static YesNoDialog newInstance(String id, int titleId, int messageId) {
        return newInstance(id, titleId, messageId, (byte) 0x0);
    }

    public static YesNoDialog newInstance(String id, int titleId, int messageId, byte dialogFlags) {
        YesNoDialog f = new YesNoDialog();
        Bundle args = new Bundle();
        args.putString(ID_KEY, id);
        args.putInt(TITLE_KEY, titleId);
        args.putInt(MESSAGE_ID_KEY, messageId);
        args.putString(MESSAGE_STRING_KEY, null);
        args.putByte(YES_NO_DIALOG_FLAGS, dialogFlags);
        f.setArguments(args);
        return f;
    }

    public static YesNoDialog newInstance(String id, int titleId, String message, byte dialogFlags) {
        YesNoDialog f = new YesNoDialog();
        Bundle args = new Bundle();
        args.putString(ID_KEY, id);
        args.putInt(TITLE_KEY, titleId);
        args.putInt(MESSAGE_ID_KEY, -1);
        args.putString(MESSAGE_STRING_KEY, message);
        args.putByte(YES_NO_DIALOG_FLAGS, dialogFlags);
        f.setArguments(args);
        return f;
    }

    @Override
    protected void initComponents(final Dialog dlg, Bundle savedInstanceState) {
        Bundle args = getArguments();

        id = args.getString(ID_KEY);

        int titleId = args.getInt(TITLE_KEY);
        int messageId = args.getInt(MESSAGE_ID_KEY);
        String messageStr = args.getString(MESSAGE_STRING_KEY);
        final byte flags = args.getByte(YES_NO_DIALOG_FLAGS);

        dlg.setContentView(R.layout.dialog_default);

        TextView defaultDialogTitle = findView(dlg, R.id.dialog_default_title);
        defaultDialogTitle.setText(titleId);

        TextView defaultDialogText = findView(dlg, R.id.dialog_default_text);
        if (messageId != -1 && messageStr == null) {
            defaultDialogText.setText(messageId);
        } else if (messageStr != null && messageId == -1) {
            defaultDialogText.setText(messageStr);
        }

        Button buttonYes = findView(dlg, R.id.dialog_default_button_yes);
        buttonYes.setText(android.R.string.yes);
        buttonYes.setOnClickListener(v -> {
            if ((flags & FLAG_DISMISS_ON_OK_BEFORE_PERFORM_DIALOG_CLICK) == FLAG_DISMISS_ON_OK_BEFORE_PERFORM_DIALOG_CLICK) {
                dlg.dismiss();
            }
            performDialogClick(id, Dialog.BUTTON_POSITIVE);
        });

        Button buttonNo = findView(dlg, R.id.dialog_default_button_no);
        buttonNo.setText(android.R.string.no);
        buttonNo.setOnClickListener(v -> {
            dlg.dismiss();
            performDialogClick(id, Dialog.BUTTON_NEGATIVE);
        });
    }

    @Override
    protected void performDialogClick(String tag, int which) {
        super.performDialogClick(id, which);
    }
}
