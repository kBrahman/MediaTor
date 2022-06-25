package z.zer.tor.media.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Util class to provide SSL/TLS specific features.
 */
public final class Ssl {

    private static final X509TrustManager NULL_TRUST_MANAGER = new NullTrustManager();

    private static final SSLSocketFactory NULL_SOCKET_FACTORY = buildNullSSLSocketFactory();

    private Ssl() {
    }

    /**
     * Returns a X509TrustManager instance that simply accepts all certificates
     * as valid.
     * <p>
     * The instance returned is always the same and it's thread safe.
     *
     * @return the hostname verifier.
     */
    public static X509TrustManager nullTrustManager() {
        return NULL_TRUST_MANAGER;
    }

    /**
     * Returns a SSLSocketFactory instance that simply accepts all certificates
     * as valid.
     * <p>
     * The instance returned is always the same and it's thread safe.
     *
     * @return the hostname verifier.
     */
    public static SSLSocketFactory nullSocketFactory() {
        return NULL_SOCKET_FACTORY;
    }

    private static SSLSocketFactory buildNullSSLSocketFactory() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{nullTrustManager()}, new SecureRandom());
            SSLSocketFactory d = sc.getSocketFactory();
            return new WrapSSLSocketFactory(d);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final class NullTrustManager implements X509TrustManager {

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                final X509TrustManager origTrustManager = (X509TrustManager) trustManagers[0];
                origTrustManager.checkServerTrusted(certs, authType);
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is a trivial wrapper of a real SSLSocketFactory as a
     * workaround to a bug in android
     */
    private static final class WrapSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory d;

        WrapSSLSocketFactory(SSLSocketFactory d) {
            this.d = d;
        }

        @Override
        public Socket createSocket(String s, int i) throws IOException {
            return d.createSocket(s, i);
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
            return d.createSocket(s, i, inetAddress, i1);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return d.createSocket(inetAddress, i);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return d.createSocket(inetAddress, i, inetAddress1, i1);
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return d.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return d.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
            return d.createSocket(socket, s, i, b);
        }
    }
}
