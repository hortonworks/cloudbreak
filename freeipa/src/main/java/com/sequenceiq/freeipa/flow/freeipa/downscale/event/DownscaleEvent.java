package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class DownscaleEvent extends StackEvent {
    private final List<String> instanceIds;

    private final String operationId;

    public DownscaleEvent(Long stackId, List<String> instanceIds) {
        this(null, stackId, instanceIds, null);
    }

    public DownscaleEvent(Long stackId, List<String> instanceIds, String operationId) {
        this(null, stackId, instanceIds, operationId);
    }

    public DownscaleEvent(String selector, Long stackId, List<String> instanceIds, String operationId) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
        this.operationId = operationId;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public String getOperationId() {
        return operationId;
    }
}
