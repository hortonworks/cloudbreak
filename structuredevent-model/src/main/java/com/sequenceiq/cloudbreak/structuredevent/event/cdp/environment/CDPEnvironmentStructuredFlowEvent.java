package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public class CDPEnvironmentStructuredFlowEvent extends CDPStructuredFlowEvent<EnvironmentDetails> {

    public CDPEnvironmentStructuredFlowEvent() {
    }

    public CDPEnvironmentStructuredFlowEvent(CDPOperationDetails operation, FlowDetails flow,
            EnvironmentDetails payload, String status, String statusReason) {
        super(operation, flow, payload, status, statusReason);
    }

}
