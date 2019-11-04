package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Set;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AmbariHostsCheckerContext extends StackContext {

    private final AmbariClient ambariClient;

    private final Set<HostMetadata> hostsInCluster;

    private final int hostCount;

    public AmbariHostsCheckerContext(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster, int hostCount) {
        super(stack);
        this.ambariClient = ambariClient;
        this.hostsInCluster = hostsInCluster;
        this.hostCount = hostCount;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Set<HostMetadata> getHostsInCluster() {
        return hostsInCluster;
    }

    public int getHostCount() {
        return hostCount;
    }
}
