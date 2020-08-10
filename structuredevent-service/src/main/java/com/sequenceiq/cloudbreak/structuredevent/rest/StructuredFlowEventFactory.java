package com.sequenceiq.cloudbreak.structuredevent.rest;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

public interface StructuredFlowEventFactory {

    StructuredFlowEvent createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed);

    StructuredFlowEvent createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception);

    StructuredNotificationEvent createStructuredNotificationEvent(Long resourceId, String notificationType, String message, String groupName);
}
