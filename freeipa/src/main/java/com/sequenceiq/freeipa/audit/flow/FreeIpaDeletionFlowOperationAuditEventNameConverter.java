package com.sequenceiq.freeipa.audit.flow;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class FreeIpaDeletionFlowOperationAuditEventNameConverter implements FreeIpaFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return TERMINATION_EVENT.name().equals(flow.getFlowEvent());
    }

    @Override
    public boolean isFinal(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return TERMINATION_FINALIZED_EVENT.name().equals(flowEvent);
    }

    @Override
    public boolean isFailed(CDPStructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return STACK_TERMINATION_FAIL_HANDLED_EVENT.name().equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.DELETE_FREEIPA;
    }
}
