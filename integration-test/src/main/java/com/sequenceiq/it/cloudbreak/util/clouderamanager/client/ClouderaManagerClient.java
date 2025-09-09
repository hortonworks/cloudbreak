package com.sequenceiq.it.cloudbreak.util.clouderamanager.client;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;

import okhttp3.OkHttpClient;

@Component
public class ClouderaManagerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClient.class);

    @Inject
    private CmRequestIdProviderInterceptor cmRequestIdProviderInterceptor;

    public ClouderaManagerClient() {
    }

    public ApiClient getCmApiClient(String serverFqdn, String clusterName, String apiVersion, String cmUser, String cmPassword) {
        String basePath = "https://" + serverFqdn + "/" + clusterName + "/cdp-proxy-api/cm-api" + apiVersion;
        return getApiClient(serverFqdn, cmUser, cmPassword, basePath);
    }

    public ApiClient getCmApiClientDirect(String serverFqdn, String clusterName, String apiVersion, String cmUser, String cmPassword) {
        String basePath = "https://" + serverFqdn + "/clouderamanager/api/" + apiVersion;
        return getApiClient(serverFqdn, cmUser, cmPassword, basePath);
    }

    private ApiClient getApiClient(String serverFqdn, String cmUser, String cmPassword, String basePath) {
        LOGGER.info(String.format("Cloudera Manager Server access details: %nserverFqdn: %s %ncmUser: %s %ncmPassword: %s", serverFqdn, cmUser, cmPassword));
        ApiClient cmClient = new ApiClient();
        cmClient.setBasePath(basePath);
        cmClient.setUsername(cmUser);
        cmClient.setPassword(cmPassword);
        OkHttpClient.Builder builder = cmClient.getHttpClient().newBuilder();
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] trustManagers = trustAllCertsTrustManager();
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            builder.hostnameVerifier((hostname, session) -> true);
            builder.addInterceptor(cmRequestIdProviderInterceptor);
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create a SSL context that trusts all certificates", e);
        }
        cmClient.setHttpClient(builder.build());
        LOGGER.info(String.format("Cloudera Manager Base Path: %s", cmClient.getBasePath()));
        return cmClient;
    }

    public ApiClient getCmApiClientWithTimeoutDisabled(String serverFqdn, String clusterName, String apiVersion, String cmUser, String cmPassword) {
        ApiClient cmClient = getCmApiClient(serverFqdn, clusterName, apiVersion, cmUser, cmPassword);
        OkHttpClient.Builder builder = cmClient.getHttpClient().newBuilder();
        builder.connectTimeout(0, TimeUnit.MILLISECONDS);
        builder.readTimeout(0, TimeUnit.MILLISECONDS);
        cmClient.setHttpClient(builder.build());
        return cmClient;
    }

    public ApiClient getCmApiClientWithTimeoutDisabledDirect(String serverFqdn, String clusterName, String apiVersion, String cmUser, String cmPassword) {
        ApiClient cmClient = getCmApiClientDirect(serverFqdn, clusterName, apiVersion, cmUser, cmPassword);
        OkHttpClient.Builder builder = cmClient.getHttpClient().newBuilder();
        builder.connectTimeout(0, TimeUnit.MILLISECONDS);
        builder.readTimeout(0, TimeUnit.MILLISECONDS);
        cmClient.setHttpClient(builder.build());
        return cmClient;
    }

    private TrustManager[] trustAllCertsTrustManager() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }
}
