package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class FreeipaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    protected FreeipaCreationHandler(EventSender eventSender, EnvironmentService environmentService) {
        super(eventSender);
        this.environmentService = environmentService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        // TODO: create freeipa
        environmentService.findById(environmentDto.getId()).ifPresent(environment -> {
            environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
            environmentService.save(environment);
        });
        sleepForTestPurpose();
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }
}
