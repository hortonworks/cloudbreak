package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;

public class AmbariHosts {

    private Long stackId;
    private AmbariClient ambariClient;
    private int hostCount;

    public AmbariHosts(Long stackId, AmbariClient ambariClient, int hostCount) {
        this.stackId = stackId;
        this.ambariClient = ambariClient;
        this.hostCount = hostCount;
    }

    public Long getStackId() {
        return stackId;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public int getHostCount() {
        return hostCount;
    }

}
