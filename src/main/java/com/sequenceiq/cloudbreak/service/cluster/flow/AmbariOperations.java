package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariOperations {

    private Stack stack;
    private AmbariClient ambariClient;
    private Map<String, Integer> requests;

    public AmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests) {
        this.stack = stack;
        this.ambariClient = ambariClient;
        this.requests = requests;
    }

    public Stack getStack() {
        return stack;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Map<String, Integer> getRequests() {
        return requests;
    }

}
