package com.sequenceiq.cloudbreak.service.stack.flow;

public class ConsulClientConfig {
    private static final String DEFAULT_PRIVATE_KEY_NAME = "/key.pem";
    private static final String DEFAULT_CLIENT_CERT_NAME = "/cert.pem";
    private static final String DEFAULT_SERVER_CERT_NAME = "/ca.pem";

    private String apiAddress;
    private String serverCert;
    private String clientCert;
    private String clientKey;

    public ConsulClientConfig(String apiAddress, String certDir) {
        this.apiAddress = apiAddress;
        this.serverCert = certDir + DEFAULT_SERVER_CERT_NAME;
        this.clientCert = certDir + DEFAULT_CLIENT_CERT_NAME;
        this.clientKey = certDir + DEFAULT_PRIVATE_KEY_NAME;
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
}