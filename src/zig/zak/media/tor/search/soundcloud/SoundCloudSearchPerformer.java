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

package zig.zak.media.tor.search.soundcloud;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

import zig.zak.media.tor.BuildConfig;
import zig.zak.media.tor.search.PagedWebSearchPerformer;
import zig.zak.media.tor.search.SearchResult;
import zig.zak.media.tor.util.JsonUtils;

public final class SoundCloudSearchPerformer extends PagedWebSearchPerformer {

    private static final String SOUND_CLOUD_CLIENT_ID = BuildConfig.SOUND_CLOUD_KEY;
    private static final String TAG = SoundCloudSearchPerformer.class.getSimpleName();

    public SoundCloudSearchPerformer(String domainName, long token, String keywords, int timeout) {
        super(domainName, token, keywords, timeout, 1);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://api.soundcloud.com/tracks/?q=" + encodedKeywords + "&limit=50&offset=0&client_id=" + SOUND_CLOUD_CLIENT_ID;
    }

    @Override
    protected List<? extends SearchResult> searchPage(String page) {
        List<SearchResult> result = new LinkedList<>();
        Log.i(TAG, page);
        JSONArray arr;
        try {
            arr = new JSONArray(page);
            for (int i = 0; i < arr.length(); i++) {
                SoundcloudItem item = JsonUtils.toObject(arr.get(i).toString(), SoundcloudItem.class);
                if (!isStopped() && item != null) {
                    SoundCloudSearchResult sr = new SoundCloudSearchResult(item, SOUND_CLOUD_CLIENT_ID);
                    result.add(sr);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // can't use fromJson here due to the isStopped call

        return result;
    }

    public static String resolveUrl(String url) {
        return "http://api.soundcloud.com/resolve.json?url=" + url + "&client_id=" + SOUND_CLOUD_CLIENT_ID;
    }

    public static LinkedList<SoundCloudSearchResult> fromJson(String json) {
        LinkedList<SoundCloudSearchResult> r = new LinkedList<>();
        if (json.contains("\"collection\":")) {
            SoundcloudResponse obj = JsonUtils.toObject(json, SoundcloudResponse.class);

            if (obj != null && obj.collection != null) {
                for (SoundcloudItem item : obj.collection) {
                    if (item != null && item.downloadable) {
                        SoundCloudSearchResult sr = new SoundCloudSearchResult(item, SOUND_CLOUD_CLIENT_ID);
                        r.add(sr);
                    }
                }
            }
        } else if (json.contains("\"tracks\":")) {
            SoundcloudPlaylist obj = JsonUtils.toObject(json, SoundcloudPlaylist.class);

            if (obj != null && obj.tracks != null) {
                for (SoundcloudItem item : obj.tracks) {
                    if (item != null && item.downloadable) {
                        SoundCloudSearchResult sr = new SoundCloudSearchResult(item, SOUND_CLOUD_CLIENT_ID);
                        r.add(sr);
                    }
                }
            }
        } else { // assume it's a single item
            SoundcloudItem item = JsonUtils.toObject(json, SoundcloudItem.class);
            if (item != null) {
                SoundCloudSearchResult sr = new SoundCloudSearchResult(item, SOUND_CLOUD_CLIENT_ID);
                r.add(sr);
            }
        }

        return r;
    }
}
