package dev.mdalvz.dune.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.NonNull;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class Module extends AbstractModule {

    @Provides
    @Singleton
    public @NonNull HostnameVerifier provideHostnameVerifier() {
        return (hostname, session) -> true;
    }

    @Provides
    @Singleton
    public @NonNull SSLSocketFactory provideSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final X509TrustManager trustManager = new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        };
        final TrustManager[] trustManagers = new TrustManager[] { trustManager };
        final SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustManagers, new SecureRandom());
        return context.getSocketFactory();
    }

    @Provides
    @Singleton
    public @NonNull ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

}
