package com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public class CDPFreeIpaStructuredFlowEvent extends CDPStructuredFlowEvent<StackDetails> {

    public CDPFreeIpaStructuredFlowEvent() {
    }

    public CDPFreeIpaStructuredFlowEvent(CDPOperationDetails operation, FlowDetails flow,
            StackDetails payload, String status, String statusReason) {
        super(operation, flow, payload, status, statusReason);
    }
}
