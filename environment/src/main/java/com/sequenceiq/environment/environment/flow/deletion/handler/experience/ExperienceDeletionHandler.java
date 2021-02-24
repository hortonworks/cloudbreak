package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATALAKE_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClusterDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ExperienceDeletionHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceDeletionHandler.class);

    private final EntitlementService entitlementService;

    private final EnvironmentService environmentService;

    private final EnvironmentExperienceDeletionAction environmentExperienceDeletionAction;

    @Value("${environment.experience.scan.enabled}")
    private boolean experienceDeletionEnabled;

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
        LOGGER.debug("Accepting XP Delete event");
        EnvironmentDeletionDto environmentDeletionDto = environmentDeletionDtoEvent.getData();
        EnvironmentDto envDto = environmentDeletionDtoEvent.getData().getEnvironmentDto();

        try {
            if (experienceDeletionEnabled) {
                if (entitlementService.isExperienceDeletionEnabled(envDto.getAccountId())) {
                    environmentService.findEnvironmentById(envDto.getId())
                            .ifPresent(environment -> environmentExperienceDeletionAction.execute(environment, environmentDeletionDto.isForceDelete()));
                } else {
                    LOGGER.debug("Experience deletion is disabled by entitlement.");
                }
            } else {
                LOGGER.debug("Experience deletion is disabled by Spring config.");
            }
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);
            eventSender().sendEvent(envDeleteEvent, environmentDeletionDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Experience deletion failed with exception", e);
            EnvClusterDeleteFailedEvent failedEvent = EnvClusterDeleteFailedEvent.builder()
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
                .withSelector(START_DATALAKE_CLUSTERS_DELETE_EVENT.selector())
                .build();
    }

}
