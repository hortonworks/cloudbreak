package com.sequenceiq.cloudbreak.ambari.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class AmbariClientPollerObject implements StackAware {

    private final AmbariClient ambariClient;

    private final Stack stack;

    public AmbariClientPollerObject(Stack stack, AmbariClient ambariClient) {
        this.stack = stack;
        this.ambariClient = ambariClient;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Stack getStack() {
        return stack;
    }
}
