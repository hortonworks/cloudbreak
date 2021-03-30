package com.sequenceiq.environment.environment.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@Service
public class EnvironmentStatusUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStatusUpdateService.class);

    private final EnvironmentService environmentService;

    private final EventSenderService eventService;

    public EnvironmentStatusUpdateService(EnvironmentService environmentService,
            EventSenderService eventService) {
        this.environmentService = environmentService;
        this.eventService = eventService;
    }

    public EnvironmentDto updateEnvironmentStatusAndNotify(CommonContext context, Payload payload, EnvironmentStatus environmentStatus,
            ResourceEvent resourceEvent, Enum<?> envState) {
        LOGGER.info("Flow entered into {}", envState.name());
        return environmentService
                .findEnvironmentById(payload.getResourceId())
                .map(environment -> {
                    environment.setStatus(environmentStatus);
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), resourceEvent);
                    return environmentDto;
                }).orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot update status of environment, because it does not exist: %s. ", payload.getResourceId())
                ));
    }

    public EnvironmentDto updateFailedEnvironmentStatusAndNotify(CommonContext context, BaseFailedFlowEvent failedFlowEvent,
            EnvironmentStatus environmentStatus, ResourceEvent resourceEvent, Enum<?> envState) {
        LOGGER.info("Flow entered into {}", envState.name());
        return environmentService
                .findEnvironmentById(failedFlowEvent.getResourceId())
                .map(environment -> {
                    environment.setStatus(environmentStatus);
                    environment.setStatusReason(failedFlowEvent.getException().getMessage());
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), resourceEvent);
                    return environmentDto;
                }).orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot update status of environment, because it does not exist: %s. ", failedFlowEvent.getResourceId())
                ));
    }

    public void updateEnvironmentStatusAndNotify(Environment environment, EnvironmentStatus environmentStatus, ResourceEvent resourceEvent) {
        LOGGER.info("Update environment status from {} to {} and notify", environment.getStatus().name(), environmentStatus.name());
        environment.setStatus(environmentStatus);
        Environment env = environmentService.save(environment);
        EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
        eventService.sendEventAndNotification(environmentDto, env.getCreator(), resourceEvent, Set.of(environmentStatus));
    }
}
