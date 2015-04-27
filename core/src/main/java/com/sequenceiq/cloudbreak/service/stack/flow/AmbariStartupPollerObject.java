package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariStartupPollerObject extends StackContext {

    private String ambariIp;
    private AmbariClient ambariClient;

    public AmbariStartupPollerObject(Stack stack, String ambariIp, AmbariClient ambariClient) {
        super(stack);
        this.ambariIp = ambariIp;
        this.ambariClient = ambariClient;
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
