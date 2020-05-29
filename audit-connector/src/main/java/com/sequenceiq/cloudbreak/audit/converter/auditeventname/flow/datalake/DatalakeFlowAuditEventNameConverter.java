package com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow.datalake;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow.FlowResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class DatalakeFlowAuditEventNameConverter implements FlowResourceAuditEventConverter {

    @Inject
    private List<DatalakeFlowOperationAuditEventNameConverter> converters;

    @Override
    public AuditEventName auditEventName(StructuredFlowEvent structuredEvent) {
        Optional<DatalakeFlowOperationAuditEventNameConverter> converter = converters.stream()
                .filter(c -> c.isInit(structuredEvent) || c.isFailed(structuredEvent) || c.isFinal(structuredEvent))
                .findFirst();
        return converter.map(DatalakeFlowOperationAuditEventNameConverter::eventName).orElse(null);
    }

    @Override
    public boolean shouldAudit(StructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return "INIT_STATE".equals(flow.getFlowState()) || "FINAL_STATE".equals(flow.getNextFlowState());
    }
}
