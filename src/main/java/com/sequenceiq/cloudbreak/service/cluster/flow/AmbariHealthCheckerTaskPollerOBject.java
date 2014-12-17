package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariHealthCheckerTaskPollerObject {

    private Stack stack;
    private AmbariClient ambariClient;

    public AmbariHealthCheckerTaskPollerObject(Stack stack, AmbariClient ambariClient) {
        this.stack = stack;
        this.ambariClient = ambariClient;
    }

    public Stack getStack() {
        return stack;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }
}
