package z.zer.tor.media.util.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import z.zer.tor.media.util.Logger;
import z.zer.tor.media.util.UserAgentGenerator;

public abstract class AbstractHttpClient implements HttpClient {
    private static final Logger LOG = Logger.getLogger(AbstractHttpClient.class);
    protected static final int DEFAULT_TIMEOUT = 10000;
    protected static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();
    protected HttpClientListener listener;
    protected boolean canceled = false;

    @Override
    public void setListener(HttpClientListener listener) {
        this.listener = listener;
    }

    @Override
    public HttpClientListener getListener() {
        return listener;
    }

    @Override
    public void onCancel() {
        if (getListener() != null) {
            try {
                getListener().onCancel(this);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onData(byte[] b, int i, int n) {
        if (getListener() != null) {
            getListener().onData(this, b, 0, n);
        }
    }

    @Override
    public void onError(Exception e) {
        if (getListener() != null) {
            try {
                getListener().onError(this, e);
            } catch (Exception e2) {
                LOG.warn(e2.getMessage());
            }
        } else {
            e.printStackTrace();
        }
    }

    @Override
    public void onComplete() {
        if (getListener() != null) {
            try {
                getListener().onComplete(this);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    abstract public int head(String url, int connectTimeoutInMillis, Map<String, List<String>> outputHeaders) throws IOException;

    @Override
    public String get(String url) throws IOException {
        return get(url, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    @Override
    public String get(String url, int timeout) throws IOException {
        return get(url, timeout, DEFAULT_USER_AGENT);
    }

    @Override
    public String get(String url, int timeout, String userAgent) throws IOException {
        return get(url, timeout, userAgent, null, null);
    }

    @Override
    public String get(String url, int timeout, String userAgent, String referrer, String cookie) throws IOException {
        return get(url, timeout, userAgent, referrer, cookie, null);
    }

    @Override
    abstract public String get(String url, int timeout, String userAgent, String referrer, String cookie, Map<String, String> customHeaders) throws IOException;

    @Override
    public void save(String url, File file) throws IOException {
        save(url, file, false, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    @Override
    public void save(String url, File file, boolean resume) throws IOException {
        save(url, file, resume, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    @Override
    public void save(String url, File file, boolean resume, int timeout, String userAgent) throws IOException {
        save(url, file, resume, timeout, userAgent, null);
    }

    @Override
    abstract public void save(String url, File file, boolean resume, int timeout, String userAgent, String referrer) throws IOException;

    @Override
    abstract public String post(String url, int timeout, String userAgent, Map<String, String> formData) throws IOException;

    @Override
    public String post(String url, int timeout, String userAgent, String content, boolean gzip) throws IOException {
        return post(url, timeout, userAgent, content, "text/plain", gzip);
    }

    abstract public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException;

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    protected byte[] getFormDataBytes(Map<String, String> formData) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (formData != null && formData.size() > 0) {
            for (Map.Entry<String, String> kv : formData.entrySet()) {
                sb.append("&");
                sb.append(kv.getKey());
                sb.append("=");
                sb.append(kv.getValue());
            }
            sb.deleteCharAt(0);
        }
        return sb.toString().getBytes("UTF-8");
    }

    protected static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    protected static void copyMultiMap(Map<String, List<String>> origin, Map<String, List<String>> destination) {
        if (origin == null || destination == null) {
            return;
        }

        for (String key : origin.keySet()) {
            destination.put(key, origin.get(key));
        }
    }
}
