package com.sequenceiq.it.cloudbreak.util.clouderamanager.client;

import java.util.concurrent.TimeUnit;

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
        builder.addInterceptor(cmRequestIdProviderInterceptor);
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
}
