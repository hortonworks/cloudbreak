package com.sequenceiq.freeipa.audit.flow;

import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FINISHED_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class FreeIpaProvisionFlowOperationAuditEventNameConverter implements FreeIpaFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return FREEIPA_PROVISION_EVENT.name().equals(flow.getFlowEvent());
    }

    @Override
    public boolean isFinal(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return FREEIPA_PROVISION_FINISHED_EVENT.name().equals(flow.getFlowEvent());
    }

    @Override
    public boolean isFailed(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return FREEIPA_PROVISION_FAILURE_HANDLED_EVENT.name().equals(flow.getFlowEvent());
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.CREATE_FREEIPA;
    }
}
