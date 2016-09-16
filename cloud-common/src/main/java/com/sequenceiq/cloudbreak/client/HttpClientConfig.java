package com.sequenceiq.cloudbreak.client;

public class HttpClientConfig {
    private static final String DEFAULT_PRIVATE_KEY_NAME = "/key.pem";
    private static final String DEFAULT_CLIENT_CERT_NAME = "/cert.pem";
    private static final String DEFAULT_SERVER_CERT_NAME = "/ca.pem";

    private String apiAddress;
    private Integer apiPort;
    private String serverCert;
    private String clientCert;
    private String clientKey;

    public HttpClientConfig(String apiAddress, Integer apiPort) {
        this.apiAddress = apiAddress;
        this.apiPort = apiPort;
    }

    public HttpClientConfig(String apiAddress, Integer apiPort, String certDir) {
        this.apiAddress = apiAddress;
        this.apiPort = apiPort;
        if (certDir != null) {
            this.serverCert = certDir + DEFAULT_SERVER_CERT_NAME;
            this.clientCert = certDir + DEFAULT_CLIENT_CERT_NAME;
            this.clientKey = certDir + DEFAULT_PRIVATE_KEY_NAME;
        }
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public Integer getApiPort() {
        return apiPort;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getServerCert() {
        return serverCert;
    }

    public String getCertDir() {
        if (serverCert != null) {
            return serverCert.substring(0, serverCert.indexOf(DEFAULT_SERVER_CERT_NAME));
        }
        return null;
    }
}