package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationType;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class AmbariOperations implements StackAware {

    private final AmbariClient ambariClient;

    private final AmbariOperationType ambariOperationType;

    private final Stack stack;

    private Map<String, Integer> requests;

    private String requestContext;

    private String requestStatus;

    public AmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests, AmbariOperationType ambariOperationType) {
        this.stack = stack;
        this.ambariClient = ambariClient;
        this.requests = requests;
        this.ambariOperationType = ambariOperationType;
    }

    public AmbariOperations(Stack stack, AmbariClient ambariClient, String requestContext, String requestStatus, AmbariOperationType ambariOperationType) {
        this.stack = stack;
        this.ambariClient = ambariClient;
        this.requestContext = requestContext;
        this.requestStatus = requestStatus;
        this.ambariOperationType = ambariOperationType;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public AmbariOperationType getAmbariOperationType() {
        return ambariOperationType;
    }

    public Map<String, Integer> getRequests() {
        return requests;
    }

    public String getRequestContext() {
        return requestContext;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public Stack getStack() {
        return stack;
    }
}
