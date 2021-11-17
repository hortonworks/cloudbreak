package com.sequenceiq.cloudbreak.polling.nginx;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;

import com.sequenceiq.cloudbreak.client.CertificateTrustManager.SavingX509TrustManager;

public class NginxPollerObject  {

    private final Client client;

    private final String ip;

    private final int gatewayPort;

    private final SavingX509TrustManager trustManager;

    private final Supplier<Boolean> isDeletingProvider;

    public NginxPollerObject(Client client, String ip, int gatewayPort, SavingX509TrustManager trustManager, Supplier<Boolean> isDeletingProvider) {
        this.client = client;
        this.ip = ip;
        this.gatewayPort = gatewayPort;
        this.trustManager = trustManager;
        this.isDeletingProvider = isDeletingProvider;
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

    public Supplier<Boolean> getIsDeletingProvider() {
        return isDeletingProvider;
    }
}
