package z.zer.tor.media.android.gui.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public final class DangerousPermissionsChecker implements ActivityCompat.OnRequestPermissionsResultCallback {

    public interface OnPermissionsGrantedCallback {
        void onPermissionsGranted();
    }

    private static final Logger LOG = Logger.getLogger(DangerousPermissionsChecker.class);
    public static final int WRITE_SETTINGS_PERMISSIONS_REQUEST_CODE = 0x000B;
    public static final int ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE = 0x000C;

    // HACK: just couldn't find another way, and this saved a lot of overcomplicated logic in the onActivityResult handling activities.
    static long AUDIO_ID_FOR_WRITE_SETTINGS_RINGTONE_CALLBACK = -1;
    static byte FILE_TYPE_FOR_WRITE_SETTINGS_RINGTONE_CALLBACK = -1;

    private final WeakReference<Activity> activityRef;
    private final int requestCode;
    private OnPermissionsGrantedCallback onPermissionsGrantedCallback;

    public DangerousPermissionsChecker(Activity activity, int requestCode) {
        if (activity instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
            this.requestCode = requestCode;
            this.activityRef = Ref.weak(activity);
        } else {
            throw new IllegalArgumentException("The activity must implement ActivityCompat.OnRequestPermissionsResultCallback");
        }
    }

    public Activity getActivity() {
        if (Ref.alive(activityRef)) {
            return activityRef.get();
        }
        return null;
    }

    public void requestPermissions() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        Activity activity = activityRef.get();
        String[] permissions = null;
        switch (requestCode) {
            case WRITE_SETTINGS_PERMISSIONS_REQUEST_CODE:
                requestWriteSettingsPermissionsAPILevel23(activity);
                break;
            // this didn't fly on my Android with API Level 23
                // it might fly on previous versions.
            case ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE:
                permissions = new String[] { Manifest.permission.ACCESS_COARSE_LOCATION };
                break;
        }

        if (permissions != null) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permissionWasGranted = false;
        switch (requestCode) {
            case WRITE_SETTINGS_PERMISSIONS_REQUEST_CODE:
                permissionWasGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case ACCESS_COARSE_LOCATION_PERMISSIONS_REQUEST_CODE:
                permissionWasGranted = onAccessCoarseLocationPermissionsResult(permissions, grantResults);
            default:
                break;
        }

        if (this.onPermissionsGrantedCallback != null && permissionWasGranted) {
            onPermissionsGrantedCallback.onPermissionsGranted();
        }
    }

    // EXTERNAL STORAGE PERMISSIONS

    public static boolean hasPermissionToWriteSettings(Context context) {
        return DangerousPermissionsChecker.canWriteSettingsAPILevel23(context);
    }

    private static boolean canWriteSettingsAPILevel23(Context context) {
        if (context == null) {
            return false;
        }
        try {
            final Class<?> SystemClass = android.provider.Settings.System.class;
            final Method canWriteMethod = SystemClass.getMethod("canWrite", Context.class);
            return (boolean) canWriteMethod.invoke(null, context);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
        return false;
    }

    /**
     * This method will invoke an activity that shows the WRITE_SETTINGS capabilities
     * of our app.
     *
     * More unnecessary distractions and time wasting for developers
     * courtesy of Google.
     *
     * https://commonsware.com/blog/2015/08/17/random-musings-android-6p0-sdk.html
     *
     * > Several interesting new Settings screens are now accessible
     * > via Settings action strings. One that will get a lot of
     * > attention is ACTION_MANAGE_WRITE_SETTINGS, where users can indicate
     * > whether apps can write to system settings or not.
     * > If your app requests the WRITE_SETTINGS permission, you may appear
     * > on this list, and you can call canWrite() on Settings.System to
     * > see if you were granted permission.
     *
     * Google geniuses, Make up your minds please.
     */
    private void requestWriteSettingsPermissionsAPILevel23(Activity activity) {
        // Settings.ACTION_MANAGE_WRITE_SETTINGS - won't build if the
        // intellij sdk is set to API 16 Platform, so I'll just hardcode
        // the value.
        // Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        Intent intent = new Intent("android.settings.action.MANAGE_WRITE_SETTINGS");
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, DangerousPermissionsChecker.WRITE_SETTINGS_PERMISSIONS_REQUEST_CODE);
    }

    private boolean onAccessCoarseLocationPermissionsResult(String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                LOG.info("ACCESS_COARSE_LOCATION permission granted? " + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

}
