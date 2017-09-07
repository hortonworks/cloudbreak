package com.sequenceiq.periscope.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.domain.Cluster;

public class ClusterCreationEvaluatorContext {

    private final AutoscaleStackResponse stack;

    private final Optional<Cluster> clusterOptional;

    public ClusterCreationEvaluatorContext(AutoscaleStackResponse stack, Optional<Cluster> clusterOptional) {
        this.stack = stack;
        this.clusterOptional = clusterOptional;
    }

    public AutoscaleStackResponse getStack() {
        return stack;
    }

    public Optional<Cluster> getClusterOptional() {
        return clusterOptional;
    }
}
