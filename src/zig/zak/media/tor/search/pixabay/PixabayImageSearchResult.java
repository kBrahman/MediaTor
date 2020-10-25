package zig.zak.media.tor.search.pixabay;

import org.apache.commons.io.FilenameUtils;

import zig.zak.media.tor.search.HttpSearchResult;

public final class PixabayImageSearchResult extends PixabayItemSearchResult implements HttpSearchResult {

    private final String displayName;
    private final String filename;

    PixabayImageSearchResult(PixabayItem item) {
        super(item);
        this.displayName = FilenameUtils.getName(item.previewURL);
        this.filename = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getThumbnailUrl() {
        return item.previewURL;
    }

    @Override
    public String getDownloadUrl() {
        return item.webformatURL;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getSize() {
        return -1;
    }
}
