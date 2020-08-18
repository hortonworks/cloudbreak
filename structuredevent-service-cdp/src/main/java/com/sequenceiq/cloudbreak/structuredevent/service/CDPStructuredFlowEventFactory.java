package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;

public interface CDPStructuredFlowEventFactory {

    CDPStructuredFlowEvent createStructuredFlowEvent(Crn resourceCrn, FlowDetails flowDetails, Boolean detailed);

//    CDPStructuredFlowEvent createStructuredFlowEvent(String resourceCrn, FlowDetails flowDetails, Boolean detailed);

    CDPStructuredFlowEvent createStructuredFlowEvent(Crn resourceCrn, FlowDetails flowDetails, Boolean detailed, Exception exception);

    CDPStructuredNotificationEvent createStructuredNotificationEvent(Long resourceId, String notificationType, String message, String groupName);
}
