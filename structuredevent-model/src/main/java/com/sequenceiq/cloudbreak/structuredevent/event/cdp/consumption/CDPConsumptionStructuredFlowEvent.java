package com.sequenceiq.cloudbreak.structuredevent.event.cdp.consumption;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public class CDPConsumptionStructuredFlowEvent extends CDPStructuredFlowEvent<ConsumptionDetails> {

    public CDPConsumptionStructuredFlowEvent() {
    }

    public CDPConsumptionStructuredFlowEvent(CDPOperationDetails operation, FlowDetails flow,
            ConsumptionDetails payload, String status, String statusReason) {
        super(operation, flow, payload, status, statusReason);
    }
}
