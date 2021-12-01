package com.sequenceiq.cloudbreak.polling.nginx;

import javax.ws.rs.client.Client;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager.SavingX509TrustManager;

public class NginxPollerObject  {

    private final Client client;

    private final String ip;

    private final int gatewayPort;

    private final SavingX509TrustManager trustManager;

    public NginxPollerObject(Client client, String ip, int gatewayPort, SavingX509TrustManager trustManager) {
        this.client = client;
        this.ip = ip;
        this.gatewayPort = gatewayPort;
        this.trustManager = trustManager;
    }

    public Client getClient() {
        return client;
    }

    public String getIp() {
        return ip;
    }

    public int getGatewayPort() {
        return gatewayPort;
    }

    public SavingX509TrustManager getTrustManager() {
        return trustManager;
    }
}
