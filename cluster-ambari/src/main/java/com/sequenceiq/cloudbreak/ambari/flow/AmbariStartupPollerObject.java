package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.List;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class AmbariStartupPollerObject implements StackAware {

    private String ambariIp;

    private List<AmbariClient> ambariClients;

    private final Stack stack;

    public AmbariStartupPollerObject(Stack stack, String ambariIp, List<AmbariClient> ambariClients) {
        this.stack = stack;
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

    @Override
    public Stack getStack() {
        return stack;
    }
}
