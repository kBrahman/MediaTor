package zig.zak.media.tor.search.soundcloud;

import androidx.annotation.Keep;

@Keep
final class SoundcloudItem {

    public int id;
    public SoundcloundUser user;
    public Media media;
    public int duration;
    public String permalink;
    public String title;
    public String permalink_url;
    public String artwork_url;
    public String created_at;
    public boolean downloadable;
}
