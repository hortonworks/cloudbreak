package com.sequenceiq.cloudbreak.structuredevent.event.cdp.redbeams;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public class CDPRedbeamsStructuredFlowEvent extends CDPStructuredFlowEvent<RedbeamsDetails> {

    public CDPRedbeamsStructuredFlowEvent() {
    }

    public CDPRedbeamsStructuredFlowEvent(CDPOperationDetails operation, FlowDetails flow,
            RedbeamsDetails payload, String status, String statusReason) {
        super(operation, flow, payload, status, statusReason);
    }
}
