package com.sequenceiq.environment.env.service;

import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.env.Environment;
import com.sequenceiq.environment.env.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class EnvironmentCreationService {

    private final EnvEntityConverter converter;

    private final EventSender eventSender;

    public EnvironmentCreationService(EnvEntityConverter converter, EventSender eventSender) {
        this.converter = converter;
        this.eventSender = eventSender;
    }

    public EnvironmentDto createEnvironment(EnvironmentDto environmentDto) {
        Environment environment = converter.dtoToEntity(environmentDto);
        environment.setId(1L);
        // TODO: repo save
        triggerEnvironmentCreationFlow(environment);
        return environmentDto;
    }

    private void triggerEnvironmentCreationFlow(Environment environment) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .build();
        eventSender.sendEvent(envCreationEvent);
    }
}
