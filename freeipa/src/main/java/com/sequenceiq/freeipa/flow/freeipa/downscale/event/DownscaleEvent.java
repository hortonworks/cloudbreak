package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import java.util.List;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class DownscaleEvent extends StackEvent {
    private final List<String> instanceIds;

    private final boolean repair;

    private final String operationId;

    private final int instanceCountByGroup;

    public DownscaleEvent(String selector, Long stackId, List<String> instanceIds, int instanceCountByGroup, boolean repair, String operationId) {
        this(selector, stackId, instanceIds, instanceCountByGroup, repair, operationId, new Promise<>());
    }

    public DownscaleEvent(String selector, Long stackId, List<String> instanceIds, int instanceCountByGroup, boolean repair, String operationId,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.instanceIds = instanceIds;
        this.instanceCountByGroup = instanceCountByGroup;
        this.repair = repair;
        this.operationId = operationId;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public boolean isRepair() {
        return repair;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "DownscaleEvent{"
                + "stackId=" + getResourceId()
                + ", instanceIds='" + instanceIds + '\''
                + ", instanceCountByGroup=" + instanceCountByGroup
                + ", repair='" + repair + '\''
                + ", operationId='" + operationId + '\''
                + "}";
    }
}
