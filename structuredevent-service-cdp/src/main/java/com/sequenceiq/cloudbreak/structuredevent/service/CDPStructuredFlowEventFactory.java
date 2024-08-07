package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPStructuredFlowEventFactory {

    CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails);

    CDPStructuredFlowEvent createStructuredFlowEvent(Long resourceId, FlowDetails flowDetails, Exception exception);

}
