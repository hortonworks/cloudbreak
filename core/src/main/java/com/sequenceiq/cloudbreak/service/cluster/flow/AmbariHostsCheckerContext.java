package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Set;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariHostsCheckerContext extends StackContext {

    private final AmbariClient ambariClient;

    private final Set<InstanceMetaData> hostsInCluster;

    private final int hostCount;

    public AmbariHostsCheckerContext(Stack stack, AmbariClient ambariClient, Set<InstanceMetaData> hostsInCluster, int hostCount) {
        super(stack);
        this.ambariClient = ambariClient;
        this.hostsInCluster = hostsInCluster;
        this.hostCount = hostCount;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Set<InstanceMetaData> getHostsInCluster() {
        return hostsInCluster;
    }

    public int getHostCount() {
        return hostCount;
    }
}
