package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariHostsCheckerContext extends StackContext {

    private AmbariClient ambariClient;
    private int hostCount;

    public AmbariHostsCheckerContext(Stack stack, AmbariClient ambariClient, int hostCount) {
        super(stack);
        this.ambariClient = ambariClient;
        this.hostCount = hostCount;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public int getHostCount() {
        return hostCount;
    }

}
