package com.sequenceiq.cloudbreak.structuredevent.audit;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.converter.EventDataExtractor;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class AuditStructuredEventHandler<T extends StructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditStructuredEventHandler.class);

    @Inject
    private AuditClient auditClient;

    @Inject
    private Map<String, EventDataExtractor<T>> eventDataExtractorMap;

    @Override
    public String selector() {
        return AsyncAuditStructuredEventSender.AUDIT_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        try {
            T data = structuredEvent.getData();
            OperationDetails operation = data.getOperation();
            EventDataExtractor<T> extractor = eventDataExtractorMap.get(operation.getEventType().name().toLowerCase() + "EventDataExtractor");
            LOGGER.info("Extract audit event as {}", extractor);
            AuditEvent event = AuditEvent.builder()
                    .withAccountId(operation.getTenant())
                    .withActor(ActorCrn.builder().withActorCrn(operation.getUserCrn()).build())
                    .withEventData(extractor.eventData(data))
                    .withEventName(extractor.eventName(data))
                    .withEventSource(extractor.eventSource(data))
                    .withSourceIp(extractor.sourceIp(data))
                    .build();
            auditClient.createAuditEvent(event);
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Audit log is unnecessary: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Cannot perform auditing: {}", e.getMessage(), e);
        }
    }
}