package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariHosts {

    private Stack stack;
    private AmbariClient ambariClient;
    private int hostCount;

    public AmbariHosts(Stack stack, AmbariClient ambariClient, int hostCount) {
        this.stack = stack;
        this.ambariClient = ambariClient;
        this.hostCount = hostCount;
    }

    public Stack getStack() {
        return stack;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public int getHostCount() {
        return hostCount;
    }

}
