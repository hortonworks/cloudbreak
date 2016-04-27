package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariOperations extends StackContext {

    private final AmbariClient ambariClient;
    private final AmbariOperationType ambariOperationType;
    private final Boolean async;
    private Map<String, Integer> requests;
    private String requestContext;
    private String requestStatus;

    public AmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests, AmbariOperationType ambariOperationType, Boolean async) {
        super(stack);
        this.ambariClient = ambariClient;
        this.requests = requests;
        this.ambariOperationType = ambariOperationType;
        this.async = async;
    }

    public AmbariOperations(Stack stack, AmbariClient ambariClient, String requestContext, String requestStatus, AmbariOperationType ambariOperationType,
            Boolean async) {
        super(stack);
        this.ambariClient = ambariClient;
        this.requestContext = requestContext;
        this.requestStatus = requestStatus;
        this.ambariOperationType = ambariOperationType;
        this.async = async;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public AmbariOperationType getAmbariOperationType() {
        return ambariOperationType;
    }

    public Boolean isAsync() {
        return async;
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
}
