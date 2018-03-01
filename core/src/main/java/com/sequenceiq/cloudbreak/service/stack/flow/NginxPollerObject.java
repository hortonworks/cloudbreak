package com.sequenceiq.cloudbreak.service.stack.flow;

import javax.ws.rs.client.Client;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class NginxPollerObject extends StackContext {

    private final Client client;

    private final String ip;

    private final int gatewayPort;

    public NginxPollerObject(Stack stack, Client client, String ip, int gatewayPort) {
        super(stack);
        this.client = client;
        this.ip = ip;
        this.gatewayPort = gatewayPort;
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
}
