package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

public class HealthCheckFailed extends StackFailureEvent {

    private List<String> instanceIds;

    public HealthCheckFailed(Long stackId, List<String> instanceIds, Exception exception) {
        super(stackId, exception);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
