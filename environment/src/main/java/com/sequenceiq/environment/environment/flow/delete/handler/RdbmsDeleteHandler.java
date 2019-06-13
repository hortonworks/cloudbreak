package com.sequenceiq.environment.environment.flow.delete.handler;

import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_RDBMS_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class RdbmsDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    protected RdbmsDeleteHandler(EventSender eventSender, EnvironmentService environmentService) {
        super(eventSender);
        this.environmentService = environmentService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            // TODO: delete rdbms
            environmentService.findById(environmentDto.getId()).ifPresent(environment -> {
                environment.setStatus(EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS);
                environmentService.save(environment);
            });
            sleepForTestPurpose();
            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                    .withResourceId(environmentDto.getResourceId())
                    .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_RDBMS_EVENT.selector();
    }
}
