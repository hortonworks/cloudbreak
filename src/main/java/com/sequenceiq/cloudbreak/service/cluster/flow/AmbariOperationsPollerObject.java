package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariOperationsPollerObject extends StackDependentPollerObject {

    private AmbariClient ambariClient;
    private Map<String, Integer> requests;

    public AmbariOperationsPollerObject(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests) {
        super(stack);
        this.ambariClient = ambariClient;
        this.requests = requests;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Map<String, Integer> getRequests() {
        return requests;
    }

}
