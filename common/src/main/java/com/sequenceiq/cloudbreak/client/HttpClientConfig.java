package com.sequenceiq.cloudbreak.client;

import org.apache.commons.lang3.StringUtils;

public class HttpClientConfig {

    private static final String DEFAULT_CLUSTER_PROXY_SERVICE_NAME = "cb-internal";

    private final String apiAddress;

    private String serverCert;

    private String clientCert;

    private String clientKey;

    private String clusterProxyUrl;

    private String clusterProxyServiceName;

    private String clusterCrn;

    public HttpClientConfig(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    public HttpClientConfig(String apiAddress, String serverCert, String clientCert, String clientKey) {
        this.apiAddress = apiAddress;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
    }

    public HttpClientConfig withClusterProxy(String clusterProxyUrl, String clusterCrn) {
        return withClusterProxy(clusterProxyUrl, clusterCrn, DEFAULT_CLUSTER_PROXY_SERVICE_NAME);
    }

    public HttpClientConfig withClusterProxy(String clusterProxyUrl, String clusterCrn, String clusterProxyServiceName) {
        this.clusterProxyUrl = clusterProxyUrl;
        this.clusterCrn = clusterCrn;
        this.clusterProxyServiceName = clusterProxyServiceName;
        return this;
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public String getServerCert() {
        return serverCert;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getClusterProxyUrl() {
        return clusterProxyUrl;
    }

    public String getClusterProxyServiceName() {
        return clusterProxyServiceName;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public boolean hasSSLConfigs() {
        return this.serverCert != null || this.clientCert != null || this.clientKey != null;
    }

    public boolean isClusterProxyEnabled() {
        return StringUtils.isNoneBlank(clusterCrn, clusterProxyUrl);
    }
}