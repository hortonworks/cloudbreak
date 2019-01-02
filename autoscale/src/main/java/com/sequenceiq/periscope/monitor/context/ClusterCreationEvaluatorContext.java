package com.sequenceiq.periscope.monitor.context;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;

public class ClusterCreationEvaluatorContext implements EvaluatorContext {

    private final AutoscaleStackV4Response stack;

    public ClusterCreationEvaluatorContext(AutoscaleStackV4Response stack) {
        this.stack = stack;
    }

    @Override
    public Object getData() {
        return stack;
    }

    @Override
    public long getItemId() {
        return stack.getStackId();
    }
}
