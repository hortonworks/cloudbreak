package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import java.util.List;

public class AmbariStartupPollerObject extends StackContext {

    private String ambariIp;

    private List<AmbariClient> ambariClients;

    public AmbariStartupPollerObject(Stack stack, String ambariIp, List<AmbariClient> ambariClients) {
        super(stack);
        this.ambariIp = ambariIp;
        this.ambariClients = ambariClients;
    }

    public String getAmbariAddress() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public Iterable<AmbariClient> getAmbariClients() {
        return ambariClients;
    }

    public void setAmbariClients(List<AmbariClient> ambariClient) {
        ambariClients = ambariClient;
    }
}
