package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class StackRepairNotificationRequest extends ClusterPlatformRequest {

    private final Set<String> unhealthyInstanceIds;

    public StackRepairNotificationRequest(Long stackId, Set<String> unhealthyInstanceIds) {
        super(stackId);
        this.unhealthyInstanceIds = unhealthyInstanceIds;
    }

    @Override
    public String selector() {
        return ManualStackRepairTriggerEvent.NOTIFY_REPAIR_SERVICE_EVENT.stringRepresentation();
    }

    public Set<String> getUnhealthyInstanceIds() {
        return unhealthyInstanceIds;
    }
}
