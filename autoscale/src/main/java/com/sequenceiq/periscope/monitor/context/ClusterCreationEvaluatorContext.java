package com.sequenceiq.periscope.monitor.context;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;

public class ClusterCreationEvaluatorContext implements EvaluatorContext {

    private final AutoscaleStackResponse stack;

    public ClusterCreationEvaluatorContext(AutoscaleStackResponse stack) {
        this.stack = stack;
    }

    @Override
    public Object getData() {
        return stack;
    }
}
