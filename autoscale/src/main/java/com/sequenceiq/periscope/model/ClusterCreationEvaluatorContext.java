package com.sequenceiq.periscope.model;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;

public class ClusterCreationEvaluatorContext {

    private final AutoscaleStackResponse stack;

    public ClusterCreationEvaluatorContext(AutoscaleStackResponse stack) {
        this.stack = stack;
    }

    public AutoscaleStackResponse getStack() {
        return stack;
    }
}
