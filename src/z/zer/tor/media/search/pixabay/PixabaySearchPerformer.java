package z.zer.tor.media.search.pixabay;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import z.zer.tor.media.BuildConfig;
import z.zer.tor.media.search.CrawlPagedWebSearchPerformer;
import z.zer.tor.media.search.SearchResult;
import z.zer.tor.media.util.JsonUtils;

public final class PixabaySearchPerformer extends CrawlPagedWebSearchPerformer<PixabaySearchResult> {

    static final String API_KEY = BuildConfig.PIXA_KEY;

    public PixabaySearchPerformer(long token, String keywords, int timeout) {
        super("pixabay.com", token, keywords, timeout, 1, 2);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return String.format(Locale.getDefault(), "https://pixabay.com/api/?key=%s&q=%s&image_type=photo", API_KEY, encodedKeywords);
    }

    @Override
    protected List<? extends SearchResult> searchPage(String page) {
        return searchPage(page, true);
    }

    @Override
    protected String getCrawlUrl(PixabaySearchResult sr) {
        return sr.getDetailsUrl();
    }

    @Override
    protected List<? extends SearchResult> crawlResult(PixabaySearchResult sr, byte[] data) throws Exception {
        String json = new String(data, StandardCharsets.UTF_8);
        return searchPage(json, false);
    }

    private List<? extends SearchResult> searchPage(String page, boolean firstPass) {
        List<SearchResult> result = new LinkedList<>();

        PixabayResponse response = JsonUtils.toObject(page, PixabayResponse.class);

        if (firstPass) {
            result.add(new PixabaySearchResult(String.format(Locale.US, "https://pixabay.com/api/videos/?key=%s&q=%s&video_type=film", API_KEY, getEncodedKeywords())));
        }

        for (PixabayItem item : response.hits) {
            if (!isStopped()) {
                if (item.type.equals("photo")) {
                    PixabayImageSearchResult sr = new PixabayImageSearchResult(item);
                    result.add(sr);
                } else if (item.type.equals("film")) {
                    // check if it has the video
                    if (item.videos != null && item.videos.tiny != null) {
                        PixabayVideoSearchResult sr = new PixabayVideoSearchResult(item);
                        result.add(sr);
                    }
                }
            }
        }

        return result;
    }
}
