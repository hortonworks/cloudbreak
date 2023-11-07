package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.UNSCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class StorageConsumptionCollectionUnschedulingHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionUnschedulingHandler.class);

    private final EnvironmentService environmentService;

    private final HandlerExceptionProcessor exceptionProcessor;

    protected StorageConsumptionCollectionUnschedulingHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            HandlerExceptionProcessor exceptionProcessor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.exceptionProcessor = exceptionProcessor;
    }

    @Override
    public String selector() {
        return UNSCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        LOGGER.debug("Storage consumption collection unscheduling flow step started.");
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent nextStateEvent = getNextStateEvent(environmentDeletionDto);
        try {
            eventSender().sendEvent(nextStateEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Storage consumption collection unscheduling failed", e);
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, nextStateEvent), LOGGER, eventSender(), selector());
        }
        LOGGER.debug("Storage consumption collection unscheduling flow step finished.");
    }

    private EnvDeleteEvent getNextStateEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_RDBMS_DELETE_EVENT.selector())
                .build();
    }

}
