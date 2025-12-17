package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.CLEANUP_EVENTS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_DISTRIBUTION_LIST_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.cleanup.EnvironmentStructuredEventCleanupService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class StructuredEventCleanupHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventCleanupHandler.class);

    private HandlerExceptionProcessor exceptionProcessor;

    private final EnvironmentStructuredEventCleanupService structuredEventCleanupService;

    protected StructuredEventCleanupHandler(EventSender eventSender, HandlerExceptionProcessor exceptionProcessor,
                                            EnvironmentStructuredEventCleanupService structuredEventCleanupService) {
        super(eventSender);
        this.exceptionProcessor = exceptionProcessor;
        this.structuredEventCleanupService = structuredEventCleanupService;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        LOGGER.debug("{} has accepted the request and about to operate.", StructuredEventCleanupHandler.class.getSimpleName());
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_DISTRIBUTION_LIST_DELETE_EVENT.selector())
                .build();
        try {
            cleanUpStructuredEvents(environmentDto.getResourceCrn());
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    @Override
    public String selector() {
        return CLEANUP_EVENTS_EVENT.selector();
    }

    private void cleanUpStructuredEvents(String resourceCrn) {
        try {
            structuredEventCleanupService.cleanUpStructuredEvents(resourceCrn);
        } catch (Exception e) {
            LOGGER.warn("Structured event cleanup [based on CRN: " + resourceCrn + "] failed due to: " + e.getMessage(), e);
        }
    }

}
