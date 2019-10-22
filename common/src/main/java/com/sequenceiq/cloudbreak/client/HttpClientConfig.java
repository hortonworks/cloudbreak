package com.sequenceiq.cloudbreak.client;

public class HttpClientConfig {

    private final String apiAddress;

    private String serverCert;

    private String clientCert;

    private String clientKey;

    public HttpClientConfig(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    public HttpClientConfig(String apiAddress, String serverCert, String clientCert, String clientKey) {
        this.apiAddress = apiAddress;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
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

    public boolean hasSSLConfigs() {
        return this.serverCert != null || this.clientCert != null || this.clientKey != null;
    }
}