/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package zig.zak.media.tor.android.gui.tasks;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import zig.zak.media.tor.R;
import zig.zak.media.tor.android.gui.activity.MainActivity;
import zig.zak.media.tor.android.gui.dialogs.ConfirmSoundcloudDownloadDialog;
import zig.zak.media.tor.android.gui.util.UIUtils;
import zig.zak.media.tor.android.util.Asyncs;
import zig.zak.media.tor.search.soundcloud.SoundCloudSearchPerformer;
import zig.zak.media.tor.search.soundcloud.SoundCloudSearchResult;
import zig.zak.media.tor.util.HttpClientFactory;
import zig.zak.media.tor.util.Logger;
import zig.zak.media.tor.util.http.HttpClient;

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
