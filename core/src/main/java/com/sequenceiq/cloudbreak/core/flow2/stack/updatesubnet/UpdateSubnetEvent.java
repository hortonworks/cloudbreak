package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;

enum UpdateSubnetEvent implements FlowEvent {
    UPDATE_SUBNET_EVENT(FlowPhases.UPDATE_ALLOWED_SUBNETS.name()),
    UPDATE_SUBNET_FINISHED_EVENT(EventSelectorUtil.selector(UpdateStackResult.class)),
    UPDATE_SUBNET_FAILED_EVENT(EventSelectorUtil.failureSelector(UpdateStackResult.class)),
    FINALIZED_EVENT("UPDATESUBNETFINALIZEDEVENT"),
    FAILURE_EVENT("UPDATESUBNETFAILUREEVENT"),
    FAIL_HANDLED_EVENT("UPDATESUBNETFAILHANDLEDEVENT");

    private String stringRepresentation;

    UpdateSubnetEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}