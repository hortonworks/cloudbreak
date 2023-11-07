package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.SCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class StorageConsumptionCollectionSchedulingHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionSchedulingHandler.class);

    private final EnvironmentService environmentService;

    private final EventBus eventBus;

    protected StorageConsumptionCollectionSchedulingHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            EventBus eventBus) {
        super(eventSender);
        this.environmentService = environmentService;
        this.eventBus = eventBus;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Storage consumption collection scheduling flow step started.");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresentOrElse(environment -> {
                                goToNextState(environmentDtoEvent, environmentDto);
                            }, () -> goToFailedState(environmentDtoEvent, environmentDto,
                                    new IllegalStateException(String.format("Environment was not found with id '%s'.", environmentDto.getId())))
                    );
        } catch (Exception e) {
            LOGGER.error("Storage consumption collection scheduling failed", e);
            goToFailedState(environmentDtoEvent, environmentDto, e);
        }
        LOGGER.debug("Storage consumption collection scheduling flow step finished.");
    }

    private void goToFailedState(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto, Exception e) {
        EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                e,
                environmentDto.getResourceCrn());

        eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
    }

    private void goToNextState(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent nextStateEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(nextStateEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return SCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT.selector();
    }

}
