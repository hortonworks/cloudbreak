package com.sequenceiq.cloudbreak.service.stack.flow;

public class HttpClientConfig {
    private static final String DEFAULT_PRIVATE_KEY_NAME = "/key.pem";
    private static final String DEFAULT_CLIENT_CERT_NAME = "/cert.pem";
    private static final String DEFAULT_SERVER_CERT_NAME = "/ca.pem";

    private String apiAddress;
    private String serverCert;
    private String clientCert;
    private String clientKey;

    public HttpClientConfig(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    public HttpClientConfig(String apiAddress, String certDir) {
        this.apiAddress = apiAddress;
        if (certDir != null) {
            this.serverCert = certDir + DEFAULT_SERVER_CERT_NAME;
            this.clientCert = certDir + DEFAULT_CLIENT_CERT_NAME;
            this.clientKey = certDir + DEFAULT_PRIVATE_KEY_NAME;
        }
    }

    public String getApiAddress() {
        return apiAddress;
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