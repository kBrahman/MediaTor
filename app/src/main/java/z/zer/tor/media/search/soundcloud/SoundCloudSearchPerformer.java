package z.zer.tor.media.search.soundcloud;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import z.zer.tor.media.BuildConfig;
import z.zer.tor.media.search.PagedWebSearchPerformer;
import z.zer.tor.media.search.SearchResult;
import z.zer.tor.media.util.JsonUtils;

public final class SoundCloudSearchPerformer extends PagedWebSearchPerformer {

    public static String SOUND_CLOUD_CLIENT_ID = BuildConfig.S_C_KEY;
    private static final String TAG = SoundCloudSearchPerformer.class.getSimpleName();

    public SoundCloudSearchPerformer(String domainName, long token, String keywords, int timeout) {
        super(domainName, token, keywords, timeout, 1);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "https://api-v2.soundcloud.com/search?q=" + encodedKeywords + "&limit=50&offset=0&client_id=" + SOUND_CLOUD_CLIENT_ID;
    }

    private String buildDownloadUrl(SoundcloudItem item) {
        String downloadUrl;
        List<Tanscoding> transcodings = item.media.getTranscodings();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Tanscoding t = transcodings.stream().filter(e -> e.getUrl().endsWith("/progressive")).findAny().orElse(null);
            if (t == null) return null;
            downloadUrl = t.getUrl();
        } else {
            downloadUrl = getUrl(transcodings);
        }
        return downloadUrl + "?client_id=" + SOUND_CLOUD_CLIENT_ID;
    }

    private String getUrl(List<Tanscoding> transcodings) {
        for (Tanscoding transcoding : transcodings) {
            String url = transcoding.getUrl();
            if (url.endsWith("/progressive")) return url;
        }
        return null;
    }

    @Override
    protected List<SearchResult> parsePage(String page) {
        List<SearchResult> result = new LinkedList<>();
        JSONArray arr;
        try {
            arr = new JSONObject(page).getJSONArray("collection");
            for (int i = 0; i < arr.length(); i++) {
                SoundcloudItem item = JsonUtils.toObject(arr.get(i).toString(), SoundcloudItem.class);
                if (!isStopped() && item != null && item.media != null) {
                    String url = buildDownloadUrl(item);
                    if (url == null) continue;
                    SoundCloudSearchResult sr = new SoundCloudSearchResult(item, url);
                    result.add(sr);
                }
            }
            Log.i(TAG, "result should be redady");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, result.toString());
        return result;
    }

}
