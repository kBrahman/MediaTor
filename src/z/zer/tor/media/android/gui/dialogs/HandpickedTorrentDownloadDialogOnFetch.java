package z.zer.tor.media.android.gui.dialogs;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;

import z.zer.tor.media.android.gui.transfers.TorrentFetcherListener;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ref;

import java.lang.ref.WeakReference;


public class HandpickedTorrentDownloadDialogOnFetch implements TorrentFetcherListener {
    private final WeakReference<Context> contextRef;
    private final WeakReference<FragmentManager> fragmentManagerRef;
    private static final Logger LOG = Logger.getLogger(HandpickedTorrentDownloadDialogOnFetch.class);

    public HandpickedTorrentDownloadDialogOnFetch(Activity activity) {
        contextRef = Ref.weak((Context) activity);
        fragmentManagerRef = Ref.weak(activity.getFragmentManager());
    }

    @Override
    public void onTorrentInfoFetched(byte[] torrentInfoData, String magnetUri, long torrentFetcherDownloadTokenId) {
        createHandpickedTorrentDownloadDialog(torrentInfoData);
    }

    private void createHandpickedTorrentDownloadDialog(byte[] torrentInfoData) {
        if (!Ref.alive(contextRef) ||
            !Ref.alive(fragmentManagerRef) ||
            torrentInfoData == null || torrentInfoData.length == 0) {
            LOG.warn("Incomplete conditions to create HandpickedTorrentDownloadDialog.");
        }
    }
}
