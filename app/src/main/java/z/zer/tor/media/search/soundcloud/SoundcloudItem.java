package z.zer.tor.media.search.soundcloud;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return "SoundcloudItem{" +
                "id=" + id +
                ", user=" + user +
                ", media=" + media +
                ", duration=" + duration +
                ", permalink='" + permalink + '\'' +
                ", title='" + title + '\'' +
                ", permalink_url='" + permalink_url + '\'' +
                ", artwork_url='" + artwork_url + '\'' +
                ", created_at='" + created_at + '\'' +
                ", downloadable=" + downloadable +
                '}';
    }
}
