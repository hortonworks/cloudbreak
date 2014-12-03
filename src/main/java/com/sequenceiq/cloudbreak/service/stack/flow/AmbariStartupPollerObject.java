package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariStartupPollerObject {

    private Stack stack;
    private String ambariIp;
    private AmbariClient ambariClient;

    public AmbariStartupPollerObject(Stack stack, String ambariIp, AmbariClient ambariClient) {
        this.stack = stack;
        this.ambariIp = ambariIp;
        this.ambariClient = ambariClient;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public void setAmbariClient(AmbariClient ambariClient) {
        this.ambariClient = ambariClient;
    }
}
