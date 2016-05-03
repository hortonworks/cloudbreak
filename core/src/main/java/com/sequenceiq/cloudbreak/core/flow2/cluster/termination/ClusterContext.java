package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Cluster;

public class ClusterContext extends CommonContext {

    private final Cluster cluster;

    public ClusterContext(String flowId, Cluster cluster) {
        super(flowId);
        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }
}
