package com.sequenceiq.cloudbreak.service.stack.flow;

import javax.ws.rs.client.Client;

import com.sequenceiq.cloudbreak.certificate.CertificateTrustManager.SavingX509TrustManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class NginxPollerObject extends StackContext {

    private final Client client;

    private final String ip;

    private final int gatewayPort;

    private final SavingX509TrustManager trustManager;

    public NginxPollerObject(Stack stack, Client client, String ip, int gatewayPort, SavingX509TrustManager trustManager) {
        super(stack);
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
