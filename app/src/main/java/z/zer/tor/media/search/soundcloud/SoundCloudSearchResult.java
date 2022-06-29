package z.zer.tor.media.search.soundcloud;

import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Locale;

import z.zer.tor.media.search.AbstractFileSearchResult;
import z.zer.tor.media.search.HttpSearchResult;
import z.zer.tor.media.search.StreamableSearchResult;

@Keep
public final class SoundCloudSearchResult extends AbstractFileSearchResult implements HttpSearchResult, StreamableSearchResult {

    private static final String DATE_FORMAT = "yyyy/mm/dd HH:mm:ss Z";
    private static final String TAG = "SoundCloudSearchResult";

    private final String displayName;
    private final String username;
    private final String trackUrl;
    private final String filename;
    private final String source;
    private final String thumbnailUrl;
    private final long date;
    private final String downloadUrl;
    private final long size;

    SoundCloudSearchResult(SoundcloudItem item, String downloadUrl) {
        this.displayName = item.title;
        this.username = buildUsername(item);
        this.trackUrl = item.permalink_url;
        this.filename = item.permalink + "-soundcloud.mp3";
        this.size = buildSize(item);
        this.source = buildSource(item);

        String userAvatarUrl = null;
        if (item.user != null) {
            userAvatarUrl = item.user.avatar_url;
        }
        this.thumbnailUrl = buildThumbnailUrl(item.artwork_url != null ? item.artwork_url : userAvatarUrl);

        this.date = buildDate(item.created_at);
        this.downloadUrl = downloadUrl;
        Log.i(TAG, "sc_item=>" + item);
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getCreationTime() {
        return date;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getDetailsUrl() {
        return trackUrl;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String getStreamUrl() {
        return downloadUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    private String buildUsername(SoundcloudItem item) {
        if (item.user != null && item.user.username != null) {
            return item.user.username;
        } else {
            return "";
        }
    }

    private long buildSize(SoundcloudItem item) {
        // not bit optimized for clarity, compiler will do it
        int x = item.duration;
        int y = 128;
        return ((long) x * y) / 8;
    }

    private String buildSource(SoundcloudItem item) {
        if (item.user != null && item.user.username != null) {
            return "Soundcloud - " + item.user.username;
        } else {
            return "Soundcloud";
        }
    }

    private String buildThumbnailUrl(String str) {
        String url = null;
        if (str != null) {
            try {
                url = str.substring(0, str.indexOf("-large.")) + "-t300x300.jpg";
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    private long buildDate(String str) {
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(str).getTime();
        } catch (Throwable e) {
            return System.currentTimeMillis();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "SoundCloudSearchResult{" +
                "displayName='" + displayName + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SoundCloudSearchResult)) {
            return false;
        }
        SoundCloudSearchResult other = (SoundCloudSearchResult) o;
        return this.getDetailsUrl().equals(other.getDetailsUrl()) && this.getDisplayName().equals(other.getDisplayName()) && this.getDownloadUrl().equals(other.getDownloadUrl());
    }

    @Override
    public int hashCode() {
        return getDetailsUrl().hashCode() + getDisplayName().hashCode() + getDownloadUrl().hashCode();
    }
}
