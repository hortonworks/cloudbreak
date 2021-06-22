package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sequenceiq.cloudbreak.audit.converter.AuditEventDetailsProto;
import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.CDPEventDataExtractor;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow.CDPFlowResourceAuditEventConverter;

@Component
public class FlowCDPEventDataExtractor implements CDPEventDataExtractor<CDPStructuredFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCDPEventDataExtractor.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private Map<String, CDPFlowResourceAuditEventConverter> resourceAuditEventConverters;

    @Override
    public EventData eventData(CDPStructuredFlowEvent structuredEvent) {
        AuditEventDetailsProto.AuditEventDetails.Builder builder = AuditEventDetailsProto.AuditEventDetails.newBuilder()
                .setClusterCrn(structuredEvent.getOperation().getResourceCrn())
                .setUserCrn(structuredEvent.getOperation().getUserCrn())
                .setTimestamp(System.currentTimeMillis())
                .setEnvironmentCrn(structuredEvent.getOperation().getEnvironmentCrn())
                .setFlowState(getFlowState(structuredEvent))
                .setFlowId(structuredEvent.getFlow().getFlowId());

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder
                .registerTypeAdapter(
                    AuditEventDetailsProto.AuditEventDetails.class,
                    new AuditEventDetailsAdapter())
                .create();

        return ServiceEventData.builder()
                .withEventDetails(gson.toJson(builder.build()))
                .withVersion(cbVersion)
                .build();
    }

    private String getFlowState(CDPStructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "INIT_STATE".equals(flowState) ? structuredEvent.getFlow().getNextFlowState() : flowState;
    }

    @Override
    public AuditEventName eventName(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        String flowEvent = flow.getFlowEvent();
        AuditEventName eventName = null;
        String resourceType = structuredEvent.getOperation().getResourceType();
        CDPFlowResourceAuditEventConverter flowResourceAuditEventConverter = getConverter(resourceType);
        if (flowResourceAuditEventConverter != null) {
            eventName = flowResourceAuditEventConverter.auditEventName(structuredEvent);
        }
        if (eventName != null) {
            LOGGER.info("Flow event name: {}", eventName);
            return eventName;
        }
        String flowState = flow.getFlowState();
        String flowType = flow.getFlowType();
        throw new UnsupportedOperationException(String.format("The %s, %s and %s does not support for auditing for %s",
                flowType, flowEvent, flowState, resourceType));
    }

    @Override
    public Crn.Service eventSource(CDPStructuredFlowEvent structuredEvent) {
        return Crn.fromString(structuredEvent.getOperation().getResourceCrn()).getService();
    }

    @Override
    public String sourceIp(CDPStructuredFlowEvent structuredEvent) {
        return null;
    }

    @Override
    public boolean shouldAudit(CDPStructuredEvent structuredEvent) {
        CDPOperationDetails operation = structuredEvent.getOperation();
        CDPFlowResourceAuditEventConverter flowResourceAuditEventConverter = getConverter(operation.getResourceType());
        if (flowResourceAuditEventConverter == null) {
            return false;
        }
        boolean crn = Crn.isCrn(operation.getResourceCrn());
        return crn && flowResourceAuditEventConverter.shouldAudit((CDPStructuredFlowEvent) structuredEvent);
    }

    private CDPFlowResourceAuditEventConverter getConverter(String resourceType) {
        return resourceAuditEventConverters.get(resourceType + "FlowAuditEventNameConverter");
    }
}
