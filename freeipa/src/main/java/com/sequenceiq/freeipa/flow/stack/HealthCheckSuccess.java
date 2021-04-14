package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

public class HealthCheckSuccess extends StackEvent {

    private List<String> instanceIds;

    public HealthCheckSuccess(Long stackId, List<String> instanceIds) {
        super(stackId);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
