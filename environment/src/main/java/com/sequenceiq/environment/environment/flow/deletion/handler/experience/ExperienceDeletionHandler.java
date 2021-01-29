package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ExperienceDeletionHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private final EntitlementService entitlementService;

    private final EnvironmentService environmentService;

    private final EnvironmentExperienceDeletionAction environmentExperienceDeletionAction;

    protected ExperienceDeletionHandler(EventSender eventSender, EntitlementService entitlementService, EnvironmentService environmentService,
            EnvironmentExperienceDeletionAction environmentExperienceDeletionAction) {
        super(eventSender);
        this.entitlementService = entitlementService;
        this.environmentService = environmentService;
        this.environmentExperienceDeletionAction = environmentExperienceDeletionAction;
    }

    @Override
    public String selector() {
        return DELETE_EXPERIENCE_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDeletionDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDeletionDtoEvent.getData();
        EnvironmentDto envDto = environmentDeletionDtoEvent.getData().getEnvironmentDto();

        try {
            if (true /*entitlementService.isExperienceDeletionEnabled(envDto.getAccountId())*/) {
                environmentService.findEnvironmentById(envDto.getId())
                        .ifPresent(environmentExperienceDeletionAction::execute);
            }
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);
            eventSender().sendEvent(envDeleteEvent, environmentDeletionDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(envDto.getId())
                    .withException(e)
                    .withResourceCrn(envDto.getResourceCrn())
                    .withResourceName(envDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDeletionDtoEvent.getHeaders());
        }
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .build();
    }

}
