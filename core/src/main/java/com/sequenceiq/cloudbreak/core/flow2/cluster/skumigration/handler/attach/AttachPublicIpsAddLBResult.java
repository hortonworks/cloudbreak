package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AttachPublicIpsAddLBResult extends StackEvent implements FlowPayload {

    @JsonCreator
    public AttachPublicIpsAddLBResult(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "AttachPublicIpsAddLBResult{} " + super.toString();
    }
}
