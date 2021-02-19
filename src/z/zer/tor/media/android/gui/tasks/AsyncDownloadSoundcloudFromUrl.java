package z.zer.tor.media.android.gui.tasks;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import z.zer.tor.media.R;
import z.zer.tor.media.android.gui.activity.MainActivity;
import z.zer.tor.media.android.gui.dialogs.ConfirmSoundcloudDownloadDialog;
import z.zer.tor.media.android.gui.util.UIUtils;
import z.zer.tor.media.android.util.Asyncs;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchPerformer;
import z.zer.tor.media.search.soundcloud.SoundCloudSearchResult;
import z.zer.tor.media.util.HttpClientFactory;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.http.HttpClient;

public final class AsyncDownloadSoundcloudFromUrl {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(AsyncDownloadSoundcloudFromUrl.class);

    public AsyncDownloadSoundcloudFromUrl(Context ctx, String soundcloudUrl) {
        Asyncs.async(ctx, AsyncDownloadSoundcloudFromUrl::doInBackground, soundcloudUrl, AsyncDownloadSoundcloudFromUrl::onPostExecute);
    }

    private static List<SoundCloudSearchResult> doInBackground(final Context context, final String soundcloudUrl) {
        List<SoundCloudSearchResult> results = new ArrayList<>();
        try {
            String url = soundcloudUrl;
            if (soundcloudUrl.contains("?in=")) {
                url = soundcloudUrl.substring(0, url.indexOf("?in="));
            }
            String resolveURL = SoundCloudSearchPerformer.resolveUrl(url);
            HttpClient client = HttpClientFactory.getInstance(HttpClientFactory.HttpContext.DOWNLOAD);
            String json = client.get(resolveURL, 10000);
            results = SoundCloudSearchPerformer.fromJson(json);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return results;
    }

    private static void onPostExecute(Context ctx, final String soundcloudUrl, List<SoundCloudSearchResult> results) {
        if (ctx != null && !results.isEmpty()) {
            MainActivity activity = (MainActivity) ctx;
            ConfirmSoundcloudDownloadDialog dlg = createConfirmListDialog(ctx, results);
            dlg.show(activity.getFragmentManager());
        }
    }

    private static ConfirmSoundcloudDownloadDialog createConfirmListDialog(Context ctx, List<SoundCloudSearchResult> results) {
        String title = ctx.getString(R.string.confirm_download);
        String whatToDownload = ctx.getString((results.size() > 1) ? R.string.playlist : R.string.track);
        String totalSize = UIUtils.getBytesInHuman(getTotalSize(results));
        String text = ctx.getString(R.string.are_you_sure_you_want_to_download_the_following, whatToDownload, totalSize);

        //AbstractConfirmListDialog
        return ConfirmSoundcloudDownloadDialog.newInstance(ctx, title, text, results);
    }

    private static long getTotalSize(List<SoundCloudSearchResult> results) {
        long totalSizeInBytes = 0;
        for (SoundCloudSearchResult sr : results) {
            totalSizeInBytes += sr.getSize();
        }
        return totalSizeInBytes;
    }
}
