package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class PublicKeyCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyCreationHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentResourceService environmentResourceService;

    private final EventBus eventBus;

    protected PublicKeyCreationHandler(EventSender eventSender, EnvironmentService environmentService, EnvironmentResourceService environmentResourceService,
            EventBus eventBus) {
        super(eventSender);
        this.environmentService = environmentService;
        this.environmentResourceService = environmentResourceService;
        this.eventBus = eventBus;
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
                    boolean created = environmentResourceService.createAndUpdateSshKey(environment);
                    if (created) {
                        String publicKeyId = environment.getAuthentication().getPublicKeyId();
                        LOGGER.info("Update the environment and it's authentication with the created public SSH key id: '{}'", publicKeyId);
                        environmentService.save(environment);
                    } else {
                        LOGGER.info("The public key id could not be created for {}", environmentDto.getName());
                    }
                } else {
                    LOGGER.debug("Environment {} requested no managed public key", environment.getName());
                }
            });
            EnvCreationEvent envCreationEvent = getEnvCreateEvent(environmentDto);
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvCreationFailureEvent failedEvent =
                    new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            Event<EnvCreationFailureEvent> ev = new Event<>(environmentDtoEvent.getHeaders(), failedEvent);
            eventBus.notify(failedEvent.selector(), ev);
        }
    }

    private EnvCreationEvent getEnvCreateEvent(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_EVENT.selector())
                .build();
    }
}
