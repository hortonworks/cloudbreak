package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariOperations extends StackContext {

    private AmbariClient ambariClient;
    private Map<String, Integer> requests;

    public AmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests) {
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
