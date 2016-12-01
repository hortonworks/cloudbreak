package com.sequenceiq.periscope.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.periscope.domain.Cluster;

public class ClusterCreationEvaluatorContext {

    private StackResponse stack;

    private Optional<Cluster> clusterOptional;

    public ClusterCreationEvaluatorContext(StackResponse stack, Optional<Cluster> clusterOptional) {
        this.stack = stack;
        this.clusterOptional = clusterOptional;
    }

    public StackResponse getStack() {
        return stack;
    }

    public Optional<Cluster> getClusterOptional() {
        return clusterOptional;
    }
}
