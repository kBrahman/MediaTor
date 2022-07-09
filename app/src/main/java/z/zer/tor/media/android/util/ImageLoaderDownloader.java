package z.zer.tor.media.android.util;


import androidx.annotation.NonNull;

import com.squareup.picasso.Downloader;

import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class ImageLoaderDownloader implements Downloader {

    private final Cache cache;
    private final Call.Factory client;

    ImageLoaderDownloader(OkHttpClient client) {
        this.cache = client.cache();
        this.client = client;
    }


    @NonNull
    @Override
    public Response load(@NonNull Request request) throws IOException {
        return client.newCall(request).execute();
    }

    @Override
    public void shutdown() {
        try {
            cache.close();
        } catch (IOException ignored) {
        }
    }
}
