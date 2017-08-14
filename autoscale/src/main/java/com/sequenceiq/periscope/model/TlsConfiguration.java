package com.sequenceiq.periscope.model;

public class TlsConfiguration {

    private String clientKey;

    private String clientCert;

    private String serverCert;

    public TlsConfiguration(String clientKey, String clientCert, String serverCert) {
        this.clientKey = clientKey;
        this.clientCert = clientCert;
        this.serverCert = serverCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getServerCert() {
        return serverCert;
    }
}
