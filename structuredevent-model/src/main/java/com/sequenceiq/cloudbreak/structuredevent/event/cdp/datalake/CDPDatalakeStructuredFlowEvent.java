package com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public class CDPDatalakeStructuredFlowEvent extends CDPStructuredFlowEvent<DatalakeDetails> {

    public CDPDatalakeStructuredFlowEvent() {
    }

    public CDPDatalakeStructuredFlowEvent(CDPOperationDetails operation, FlowDetails flow,
            DatalakeDetails payload, String status, String statusReason) {
        super(operation, flow, payload, status, statusReason);
    }
}
