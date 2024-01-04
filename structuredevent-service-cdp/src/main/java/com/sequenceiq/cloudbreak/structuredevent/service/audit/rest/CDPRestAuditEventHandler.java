package com.sequenceiq.cloudbreak.structuredevent.service.audit.rest;


import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor.RequestMethodBasedRestAuditEventNameExtractor;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor.RestAuditEventSourceExtractor;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor.RestCDPEventDataExtractor;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CDPRestAuditEventHandler implements EventHandler<CDPStructuredRestCallEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDPRestAuditEventHandler.class);

    @Inject
    private AuditClient auditClient;

    @Inject
    private RestCDPEventDataExtractor extractor;

    @Inject
    private RestAuditEventSourceExtractor restAuditEventSourceExtractor;

    @Inject
    private RequestMethodBasedRestAuditEventNameExtractor eventNameExtractor;

    @Override
    public String selector() {
        return CDPRestAuditEventSender.CDP_REST_AUDIT_EVENT;
    }

    @Override
    public void accept(Event<CDPStructuredRestCallEvent> cdpStructuredRestCallEventEvent) {
        try {
            CDPStructuredRestCallEvent data = cdpStructuredRestCallEventEvent.getData();
            CDPOperationDetails operation = data.getOperation();
            String requestId = extractor.requestId(data);
            LOGGER.info("Extract rest audit event from {} with request id: '{}'", CDPStructuredRestCallEvent.class, requestId);
            AuditEvent event = AuditEvent.builder()
                    .withAccountId(operation.getAccountId())
                    .withActor(ActorCrn.builder().withActorCrn(operation.getUserCrn()).build())
                    .withEventData(extractor.eventData(data, Boolean.FALSE))
                    .withEventName(eventNameExtractor.getEventNameBasedOnRequestMethod(data))
                    .withEventSource(restAuditEventSourceExtractor.eventSource())
                    .withSourceIp(extractor.sourceIp(data))
                    .withRequestId(requestId)
                    .build();
            long clientCallStarted = System.currentTimeMillis();
            auditClient.createAuditEvent(event);
            long clientCallFinished = System.currentTimeMillis();
            LOGGER.info("Rest audit event has been sent for request id: '{}' and the gRPC call took: '{}'ms", requestId, clientCallFinished - clientCallStarted);
        } catch (Exception e) {
            LOGGER.warn("REST-AUDIT-EVENT failed to send due to: {}", e.getMessage(), e);
        }
    }
}
