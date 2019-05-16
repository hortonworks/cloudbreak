package com.sequenceiq.environment.env.flow.creation.handler;

import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationHandlerSelectors.CREATE_RDBMS_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.env.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.env.service.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class RdbmsCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    protected RdbmsCreationHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        // TODO: create rdbms
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getId())
                .withResourceName(environmentDto.getName())
                .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_RDBMS_EVENT.selector();
    }
}
