package z.zer.tor.media.android.gui.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import com.andrew.apollo.utils.MusicUtils;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import z.zer.tor.media.BuildConfig;
import z.zer.tor.media.R;
import z.zer.tor.media.android.core.ConfigurationManager;
import z.zer.tor.media.android.core.Constants;
import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.android.gui.dialogs.YesNoDialog;
import z.zer.tor.media.android.gui.views.EditTextDialog;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.MimeDetector;
import z.zer.tor.media.uxstats.UXAction;
import z.zer.tor.media.uxstats.UXStats;

public final class UIUtils {

    private static final Logger LOG = Logger.getLogger(UIUtils.class);

    /**
     * Localizable Number Format constant for the current default locale.
     */
    private static final NumberFormat NUMBER_FORMAT0; // localized "#,##0"

    private static final String[] BYTE_UNITS = new String[]{"b", "KB", "Mb", "Gb", "Tb"};

    private static final String GENERAL_UNIT_KBPSEC = "KB/s";

    // put "support" pitches at the beginning and play with the offset
    private static final int[] PITCHES = {R.string.support_frostwire, R.string.support_free_software, R.string.support_frostwire, R.string.support_free_software, R.string.save_bandwidth, R.string.cheaper_than_drinks, R.string.cheaper_than_lattes, R.string.cheaper_than_parking, R.string.cheaper_than_beer, R.string.cheaper_than_cigarettes, R.string.cheaper_than_gas, R.string.keep_the_project_alive};

    static {
        NUMBER_FORMAT0 = NumberFormat.getNumberInstance(Locale.getDefault());
        NUMBER_FORMAT0.setMaximumFractionDigits(0);
        NUMBER_FORMAT0.setMinimumFractionDigits(0);
        NUMBER_FORMAT0.setGroupingUsed(true);
    }

    private static void showToastMessage(Context context, String message, int duration, int gravity, int xOffset, int yOffset) {
        if (context != null && message != null) {
            Toast toast = Toast.makeText(context, message, duration);
            if (gravity != (Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM)) {
                toast.setGravity(gravity, xOffset, yOffset);
            }
            toast.show();
        }
    }

    public static void showShortMessage(View view, int resourceId) {
        Snackbar.make(view, resourceId, Snackbar.LENGTH_SHORT).show();
    }

    public static void showLongMessage(View view, int resourceId) {
        Snackbar.make(view, resourceId, Snackbar.LENGTH_LONG).show();
    }

    public static void showDismissableMessage(View view, int resourceId) {
        final Snackbar snackbar = Snackbar.make(view, resourceId, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(android.R.string.ok, v -> snackbar.dismiss()).show();
    }

    public static void sendShutdownIntent(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("shutdown-frostwire", true);
        ctx.startActivity(i);
    }

    public static void sendGoHomeIntent(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("gohome-frostwire", true);
        ctx.startActivity(i);
    }

    public static void showToastMessage(Context context, String message, int duration) {
        showToastMessage(context, message, duration, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    }

    public static void showShortMessage(Context context, String message) {
        showToastMessage(context, message, Toast.LENGTH_SHORT);
    }

    public static void showLongMessage(Context context, String message) {
        showToastMessage(context, message, Toast.LENGTH_LONG);
    }

    public static void showShortMessage(Context context, int resId) {
        showShortMessage(context, context.getString(resId));
    }

    public static void showLongMessage(Context context, @StringRes int resId) {
        showLongMessage(context, context.getString(resId));
    }

    public static void showShortMessage(Context context, int resId, Object... formatArgs) {
        showShortMessage(context, context.getString(resId, formatArgs));
    }

    public static void showYesNoDialog(FragmentManager fragmentManager, String message, int titleId, OnClickListener positiveListener) {
        showYesNoDialog(fragmentManager, message, titleId, positiveListener, (dialog, which) -> dialog.dismiss());
    }

    public static void showYesNoDialog(FragmentManager fragmentManager, String message, int titleId, OnClickListener positiveListener, OnClickListener negativeListener) {
        YesNoDialog yesNoDialog = YesNoDialog.newInstance(message, titleId, message, (byte) 0);
        yesNoDialog.setOnDialogClickListener((tag, which) -> {
            if (which == Dialog.BUTTON_POSITIVE && positiveListener != null) {
                positiveListener.onClick(yesNoDialog.getDialog(), which);
            } else if (which == Dialog.BUTTON_NEGATIVE && negativeListener != null) {
                negativeListener.onClick(yesNoDialog.getDialog(), which);
            }
            yesNoDialog.dismiss();
        });
        yesNoDialog.show(fragmentManager);
    }

    public static void showEditTextDialog(FragmentManager fragmentManager, int messageStringId, int titleStringId, int positiveButtonStringId, boolean cancelable, boolean multilineInput, String optionalEditTextValue, final EditTextDialog.TextViewInputDialogCallback callback) {
        new EditTextDialog().
                init(titleStringId, messageStringId, positiveButtonStringId, cancelable, multilineInput, optionalEditTextValue, callback).show(fragmentManager);
    }

    public static String getBytesInHuman(long size) {
        int i;
        float sizeFloat = (float) size;
        for (i = 0; sizeFloat > 1024; i++) {
            sizeFloat /= 1024f;
        }
        return String.format(Locale.US, "%.2f %s", sizeFloat, BYTE_UNITS[i]);
    }

    /**
     * Opens the given file with the default Android activity for that File and
     * mime type.
     */
    public static void openFile(Context context, String filePath, String mime, boolean useFileProvider) {
        try {
            if (filePath != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(getFileUri(context, filePath, useFileProvider), Intent.normalizeMimeType(mime));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (mime != null && mime.contains("video")) {
                    if (MusicUtils.isPlaying()) {
                        MusicUtils.playOrPause();
                    }
                    UXStats.instance().log(UXAction.LIBRARY_VIDEO_PLAY);
                }
                context.startActivity(i);
            }
        } catch (Throwable e) {
            UIUtils.showShortMessage(context, R.string.cant_open_file);
            LOG.error("Failed to open file: " + filePath, e);
        }
    }

    /**
     * Takes a screenshot of the given view
     *
     * @return File with jpeg of the screenshot taken. null if there was a problem.
     */
    public static File takeScreenshot(View view) {
        view.setDrawingCacheEnabled(true);
        try {
            Thread.sleep(300);
        } catch (Throwable t) {
        }
        Bitmap drawingCache = null;
        try {
            drawingCache = view.getDrawingCache();
        } catch (Throwable ignored) {
        }
        Bitmap screenshotBitmap = null;
        if (drawingCache != null) {
            try {
                screenshotBitmap = Bitmap.createBitmap(drawingCache);
            } catch (Throwable ignored) {
            }
        }
        view.setDrawingCacheEnabled(false);
        if (screenshotBitmap == null) {
            return null;
        }
        File screenshotFile = new File(Environment.getExternalStorageDirectory().toString(), "fwPlayerScreenshot.tmp.jpg");
        if (screenshotFile.exists()) {
            screenshotFile.delete();
            try {
                screenshotFile.createNewFile();
            } catch (IOException ignore) {
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(screenshotFile);
            screenshotBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Throwable t) {
            screenshotFile.delete();
            screenshotFile = null;
        }
        return screenshotFile;
    }

    public static Uri getFileUri(Context context, String filePath, boolean useFileProvider) {
        return useFileProvider ? FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", new File(filePath)) : Uri.fromFile(new File(filePath));
    }

    public static void openFile(Context context, File file) {
        openFile(context, file.getAbsolutePath(), getMimeType(file.getAbsolutePath()), true);
    }

    public static void openFile(Context context, File file, boolean useFileProvider) {
        openFile(context, file.getAbsolutePath(), getMimeType(file.getAbsolutePath()), useFileProvider);
    }

    public static void openURL(Context context, String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            // ignore
            // yes, it happens
        }
    }

    public static String getMimeType(String filePath) {
        try {
            return MimeDetector.getMimeType(FilenameUtils.getExtension(filePath));
        } catch (Throwable e) {
            LOG.error("Failed to read mime type for: " + filePath);
            return MimeDetector.UNKNOWN;
        }
    }

    /**
     * Checks setting to show or not the transfers window right after a download has started.
     * This should probably be moved elsewhere (similar to GUIMediator on the desktop)
     */
    public static void showTransfersOnDownloadStart(Context context) {
        if (ConfigurationManager.instance().showTransfersOnDownloadStart() && context != null) {
            Intent i = new Intent(context, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(i);
        }
    }

    public static void showKeyboard(Context context, View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void hideKeyboardFromActivity(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void goToFrostWireMainActivity(Activity activity) {
        final Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    // tried playing around with <T> but at the moment I only need ByteExtra's, no need to over enginner.
    public static class IntentByteExtra {
        public final String name;
        public final byte value;

        public IntentByteExtra(String name, byte value) {
            this.name = name;
            this.value = value;
        }
    }

    public static void broadcastAction(Context ctx, String actionCode, IntentByteExtra... extras) {
        if (ctx == null || actionCode == null) {
            return;
        }
        final Intent intent = new Intent(actionCode);
        if (extras != null && extras.length > 0) {
            for (IntentByteExtra extra : extras) {
                intent.putExtra(extra.name, extra.value);
            }
        }
        ctx.sendBroadcast(intent);
    }

    public static boolean isTablet(Resources res) {
        return res.getBoolean(R.bool.isTablet);
    }

}
