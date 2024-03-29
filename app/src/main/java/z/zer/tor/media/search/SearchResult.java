package z.zer.tor.media.search;

import z.zer.tor.media.licenses.License;

public interface SearchResult {

    String getDetailsUrl();

    String getDisplayName();

    long getCreationTime();

    String getSource();

    License getLicense();

    String getThumbnailUrl();

    int uid();
}
