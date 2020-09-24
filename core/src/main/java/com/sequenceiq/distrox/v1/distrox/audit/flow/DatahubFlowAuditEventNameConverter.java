package com.sequenceiq.distrox.v1.distrox.audit.flow;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.auditeventname.flow.FlowResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class DatahubFlowAuditEventNameConverter implements FlowResourceAuditEventConverter {

    @Inject
    private List<DatahubFlowOperationAuditEventNameConverter> converters;

    @Override
    public AuditEventName auditEventName(StructuredFlowEvent structuredEvent) {
        Optional<DatahubFlowOperationAuditEventNameConverter> converter = converters.stream()
                .filter(c -> c.isInit(structuredEvent) || c.isFailed(structuredEvent) || c.isFinal(structuredEvent))
                .findFirst();
        return converter.map(DatahubFlowOperationAuditEventNameConverter::eventName).orElse(null);
    }

    @Override
    public boolean shouldAudit(StructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return "INIT_STATE".equals(flow.getFlowState()) || "FINAL_STATE".equals(flow.getNextFlowState());
    }
}
