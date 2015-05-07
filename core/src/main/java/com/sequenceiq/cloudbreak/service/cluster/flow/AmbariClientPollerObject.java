package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariClientPollerObject extends StackContext {

    private final AmbariClient ambariClient;

    public AmbariClientPollerObject(Stack stack, AmbariClient ambariClient) {
        super(stack);
        this.ambariClient = ambariClient;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }
}
