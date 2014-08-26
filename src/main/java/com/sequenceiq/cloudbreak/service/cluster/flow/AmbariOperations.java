package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;

public class AmbariOperations {

    private Long stackId;
    private AmbariClient ambariClient;
    private Map<String, Integer> requests;

    public AmbariOperations(Long stackId, AmbariClient ambariClient, Map<String, Integer> requests) {
        this.stackId = stackId;
        this.ambariClient = ambariClient;
        this.requests = requests;
    }

    public Long getStackId() {
        return stackId;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Map<String, Integer> getRequests() {
        return requests;
    }

}
