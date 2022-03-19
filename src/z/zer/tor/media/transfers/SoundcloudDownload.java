package z.zer.tor.media.transfers;

import z.zer.tor.media.search.soundcloud.SoundCloudSearchResult;
import z.zer.tor.media.util.Logger;

/**
 * @author gubatron
 * @author aldenml
 */
public class SoundcloudDownload extends HttpDownload {

    private static final Logger LOG = Logger.getLogger(SoundcloudDownload.class);

    private static final long COVERART_FETCH_THRESHOLD = 20971520; //20MB

    private final SoundCloudSearchResult sr;

    public SoundcloudDownload(SoundCloudSearchResult sr) {
        super(convert(sr));
        this.sr = sr;
    }

    @Override
    protected void onFinishing() {
        super.onFinishing();
    }

    private static Info convert(SoundCloudSearchResult sr) {
        return new Info(sr.getStreamUrl(), sr.getFilename(), sr.getDisplayName(), sr.getSize());
    }
}
