package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;

public interface CDPStructuredFlowEventFactory {

    CDPStructuredFlowEvent createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed);

    CDPStructuredFlowEvent createStucturedFlowEvent(Long resourceId, FlowDetails flowDetails, Boolean detailed, Exception exception);

    CDPStructuredNotificationEvent createStructuredNotificationEvent(Long resourceId, String notificationType, String message, String groupName);
}
