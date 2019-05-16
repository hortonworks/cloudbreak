package com.sequenceiq.environment.env.flow.creation.handler;

import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.env.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.env.service.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class FreeipaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    protected FreeipaCreationHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        // TODO: create freeipa
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getId())
                .withResourceName(environmentDto.getName())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }
}
