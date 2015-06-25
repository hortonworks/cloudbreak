package com.sequenceiq.periscope.model;

public class TlsConfiguration {

    private String clientKeyPath;
    private String clientCertPath;
    private String serverCertPath;

    public TlsConfiguration(String clientKeyPath, String clientCertPath, String serverCertPath) {
        this.clientKeyPath = clientKeyPath;
        this.clientCertPath = clientCertPath;
        this.serverCertPath = serverCertPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }

    public String getServerCertPath() {
        return serverCertPath;
    }
}
