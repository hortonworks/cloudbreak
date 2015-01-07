package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariHealthCheckerPollerObject extends StackDependentPollerObject {
    private AmbariClient ambariClient;

    public AmbariHealthCheckerPollerObject(Stack stack, AmbariClient ambariClient) {
        super(stack);
        this.ambariClient = ambariClient;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }
}
