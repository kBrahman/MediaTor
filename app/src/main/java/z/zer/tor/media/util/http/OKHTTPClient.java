package z.zer.tor.media.util.http;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.Ssl;
import z.zer.tor.media.util.ThreadPool;
import z.zer.tor.media.util.Utils;


public class OKHTTPClient extends AbstractHttpClient {

    private static final Logger LOG = Logger.getLogger(OKHTTPClient.class);
    private static final String TAG = "OKHTTPClient";
    private final ThreadPool pool;

    public static final ConnectionPool CONNECTION_POOL = new ConnectionPool(5, 10, TimeUnit.SECONDS);

    public OKHTTPClient(final ThreadPool pool) {
        this.pool = pool;
    }

    @Override
    public int head(String url, int connectTimeoutInMillis, Map<String, List<String>> outputHeaders) throws IOException {
        final OkHttpClient.Builder okHttpClient = newOkHttpClient();
        okHttpClient.connectTimeout(connectTimeoutInMillis, TimeUnit.MILLISECONDS);
        okHttpClient.followRedirects(false);
        Request req = new Request.Builder().
                url(url).
                header("User-Agent", DEFAULT_USER_AGENT).
                head().
                build();
        Response resp = okHttpClient.build().newCall(req).execute();
        copyMultiMap(resp.headers().toMultimap(), outputHeaders);
        return resp.code();
    }

    @Override
    public String get(String url, int timeout, String userAgent, String referrer, String cookie, Map<String, String> customHeaders) throws IOException {
        String result;
        final OkHttpClient.Builder okHttpClient = newOkHttpClient();
        final Request.Builder builder = prepareRequestBuilder(okHttpClient, url, timeout, userAgent, referrer, cookie);
        addCustomHeaders(customHeaders, builder);
        try (Response response = getSyncResponse(okHttpClient, builder)) {
            int code = response.code();
            if (code == 401) throw new UnauthorizedException();
            Log.i(TAG, "response code=>" + code);
            result = response.body().string();
        }
        return result;
    }

    @Override
    public void save(String url, File file, boolean resume, int timeout, String userAgent, String referrer) throws IOException {
        FileOutputStream fos;
        long rangeStart;
        canceled = false;
        if (resume && file.exists()) {
            fos = new FileOutputStream(file, true);
            rangeStart = file.length();
        } else {
            fos = new FileOutputStream(file, false);
            rangeStart = -1;
        }

        final OkHttpClient.Builder okHttpClient = newOkHttpClient();
        final Request.Builder builder = prepareRequestBuilder(okHttpClient, url, timeout, userAgent, referrer, null);
        addRangeHeader(rangeStart, -1, builder);
        final Response response = getSyncResponse(okHttpClient, builder);
        final Headers headers = response.headers();
        onHeaders(headers);
        final InputStream in = response.body().byteStream();

        byte[] b = new byte[4096];
        int n;
        while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
            if (!canceled) {
                fos.write(b, 0, n);
                onData(b, 0, n);
            }
        }
        closeQuietly(fos);
        closeQuietly(response.body());
        if (canceled) {
            onCancel();
        } else {
            onComplete();
        }
    }

    private void onHeaders(Headers headers) {
        if (getListener() != null) {
            try {
                getListener().onHeaders(this, headers.toMultimap());
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public String post(String url, int timeout, String userAgent, Map<String, String> formData) throws IOException {
        return post(url, timeout, userAgent, "application/x-www-form-urlencoded; charset=utf-8", getFormDataBytes(formData), false);
    }

    @Override
    public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException {
        return post(url, timeout, userAgent, postContentType, content.getBytes("UTF-8"), gzip);
    }

    private String post(String url, int timeout, String userAgent, String postContentType, byte[] postData, boolean gzip) throws IOException {
        canceled = false;
        final OkHttpClient.Builder okHttpClient = newOkHttpClient();
        final Request.Builder builder = prepareRequestBuilder(okHttpClient, url, timeout, userAgent, null, null);
        final RequestBody requestBody = RequestBody.create(MediaType.parse(postContentType), postData);
        prepareOkHttpClientForPost(okHttpClient, gzip);
        builder.post(requestBody);
        return getPostSyncResponse(builder);
    }

    private String getPostSyncResponse(Request.Builder builder) throws IOException {
        String result = null;
        final OkHttpClient.Builder okHttpClient = newOkHttpClient();
        final Response response = this.getSyncResponse(okHttpClient, builder);
        try {
            int httpResponseCode = response.code();

            if ((httpResponseCode != HttpURLConnection.HTTP_OK) && (httpResponseCode != HttpURLConnection.HTTP_PARTIAL)) {
                throw new ResponseCodeNotSupportedException(httpResponseCode);
            }

            if (canceled) {
                onCancel();
            } else {
                result = response.body().string();
                onComplete();
            }
        } finally {
            closeQuietly(response.body());
        }

        return result;
    }

    private void prepareOkHttpClientForPost(OkHttpClient.Builder okHttpClient, boolean gzip) {
        okHttpClient.followRedirects(false);
        if (gzip) {
            if (okHttpClient.interceptors().size() > 0) {
                okHttpClient.interceptors().remove(0);
                okHttpClient.interceptors().add(0, new GzipRequestInterceptor());
            }
        }
    }

    private void addRangeHeader(long rangeStart, long rangeEnd, Request.Builder builderRef) {
        if (rangeStart < 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("bytes=");
        sb.append(String.valueOf(rangeStart));
        sb.append('-');
        if (rangeEnd > 0 && rangeEnd > rangeStart) {
            sb.append(String.valueOf(rangeEnd));
        }
        builderRef.addHeader("Range", sb.toString());
    }

    private Request.Builder prepareRequestBuilder(OkHttpClient.Builder okHttpClient, String url, int timeout, String userAgent, String referrer, String cookie) {
        okHttpClient.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.readTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.writeTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.interceptors().clear();
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (!Utils.isNullOrEmpty(userAgent)) {
            builder.header("User-Agent", userAgent);
        }
        if (!Utils.isNullOrEmpty(referrer)) {
            try {
                builder.header("Referer", referrer); // [sic - typo in HTTP protocol]
            } catch (IllegalArgumentException illegalEx) {
                LOG.info("Referer value: " + referrer);
                LOG.warn(illegalEx.getMessage(), illegalEx);
            }
        }
        if (!Utils.isNullOrEmpty(cookie)) {
            builder.header("Cookie", cookie);
        }
        return builder;
    }

    private void addCustomHeaders(Map<String, String> customHeaders, Request.Builder builder) {
        if (customHeaders != null && customHeaders.size() > 0) {
            try {
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    builder.header(header.getKey(), header.getValue());
                }
            } catch (Throwable e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private Response getSyncResponse(OkHttpClient.Builder okHttpClient, Request.Builder builder) throws IOException {
        final Request request = builder.build();
        return okHttpClient.build().newCall(request).execute();
    }

    private OkHttpClient.Builder newOkHttpClient() {
        return newOkHttpClient(pool);
    }

    public static OkHttpClient.Builder newOkHttpClient(ThreadPool pool) {
        OkHttpClient.Builder searchClient = new OkHttpClient.Builder();
        searchClient.dispatcher(new Dispatcher(pool));
        searchClient.connectionPool(CONNECTION_POOL);
        searchClient.followRedirects(true);
        searchClient.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);

        configNullSsl(searchClient);

        // Maybe we should use a custom connection pool here. Using default.
        //searchClient.setConnectionPool(?);
        return searchClient;
    }

    public static OkHttpClient.Builder configNullSsl(OkHttpClient.Builder b) {
        b.followSslRedirects(true);
//        b.hostnameVerifier(Ssl.nullHostnameVerifier());
        b.sslSocketFactory(Ssl.nullSocketFactory(), Ssl.nullTrustManager());

        ConnectionSpec spec1 = cipherSpec(ConnectionSpec.CLEARTEXT);
        ConnectionSpec spec2 = cipherSpec(ConnectionSpec.COMPATIBLE_TLS);
        ConnectionSpec spec3 = cipherSpec(ConnectionSpec.MODERN_TLS);
        b.connectionSpecs(Arrays.asList(spec1, spec2, spec3));

        return b;
    }

    private static ConnectionSpec cipherSpec(ConnectionSpec spec) {
        ConnectionSpec.Builder b = new ConnectionSpec.Builder(spec);
        if (spec.isTls()) {
            b = b.allEnabledCipherSuites();
            b = b.allEnabledTlsVersions();
            b = b.supportsTlsExtensions(true);
        }
        return b.build();
    }

    /**
     * This interceptor compresses the HTTP request body. Many web servers can't handle this!
     */
    class GzipRequestInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
                return chain.proceed(originalRequest);
            }

            Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), forceContentLength(gzip(originalRequest.body())))
                    .build();
            return chain.proceed(compressedRequest);
        }

        /**
         * https://github.com/square/okhttp/issues/350
         */
        private RequestBody forceContentLength(final RequestBody requestBody) throws IOException {
            final Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return requestBody.contentType();
                }

                @Override
                public long contentLength() {
                    return buffer.size();
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    sink.write(buffer.snapshot());
                }
            };
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return body.contentType();
                }

                @Override
                public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }
}
