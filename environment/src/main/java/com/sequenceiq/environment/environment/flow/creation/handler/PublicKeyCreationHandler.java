package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class PublicKeyCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyCreationHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentResourceService environmentResourceService;

    protected PublicKeyCreationHandler(EventSender eventSender, EnvironmentService environmentService, EnvironmentResourceService environmentResourceService) {

        super(eventSender);
        this.environmentService = environmentService;
        this.environmentResourceService = environmentResourceService;
    }

    @Override
    public String selector() {
        return CREATE_PUBLICKEY_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting PublicKeyCreation event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                if (environment.getAuthentication().isManagedKey()) {
                    environmentResourceService.createAndUpdateSshKey(environment);
                } else {
                    LOGGER.debug("Environment {} requested no managed public key", environment.getName());
                }
            });
            EnvCreationEvent envCreationEvent = getEnvCreateEvent(environmentDto);
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvCreationFailureEvent failedEvent =
                    new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    private EnvCreationEvent getEnvCreateEvent(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                .build();
    }
}
