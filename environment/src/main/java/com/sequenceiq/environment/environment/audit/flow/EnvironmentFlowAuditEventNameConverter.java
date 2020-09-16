package com.sequenceiq.environment.environment.audit.flow;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow.CDPFlowOperationAuditEventNameConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow.CDPFlowResourceAuditEventConverter;

@Component
public class EnvironmentFlowAuditEventNameConverter implements CDPFlowResourceAuditEventConverter {

    @Inject
    private List<CDPFlowOperationAuditEventNameConverter> converters;

    @Override
    public AuditEventName auditEventName(CDPStructuredFlowEvent structuredEvent) {
        Optional<CDPFlowOperationAuditEventNameConverter> converter = converters.stream()
                .filter(c -> c.isInit(structuredEvent) || c.isFailed(structuredEvent) || c.isFinal(structuredEvent))
                .findFirst();
        return converter.map(CDPFlowOperationAuditEventNameConverter::eventName).orElse(null);
    }

    @Override
    public boolean shouldAudit(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return "INIT_STATE".equals(flow.getFlowState()) || "FINAL_STATE".equals(flow.getNextFlowState());
    }
}
