package com.sequenceiq.environment.environment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.v1.EnvironmentApiConverter;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.notification.NotificationService;

@Service
public class EnvironmentStatusUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStatusUpdateService.class);

    private final EnvironmentService environmentService;

    private final NotificationService notificationService;

    private final EnvironmentApiConverter environmentApiConverter;

    public EnvironmentStatusUpdateService(EnvironmentService environmentService,
            NotificationService notificationService,
            EnvironmentApiConverter environmentApiConverter) {
        this.environmentService = environmentService;
        this.notificationService = notificationService;
        this.environmentApiConverter = environmentApiConverter;
    }

    public EnvironmentDto updateEnvironmentStatusAndNotify(CommonContext context, Payload payload, EnvironmentStatus environmentStatus,
            ResourceEvent resourceEvent, Enum envState) {
        LOGGER.info("Flow entered into {}", envState.name());
        return environmentService
                .findEnvironmentById(payload.getResourceId())
                .map(environment -> {
                    environment.setStatus(environmentStatus);
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(resourceEvent, simpleResponse, context.getFlowTriggerUserCrn());
                    return environmentDto;
                }).orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot update status of environment, because it does not exist: %s. ", payload.getResourceId())
                ));
    }

    public EnvironmentDto updateFailedEnvironmentStatusAndNotify(CommonContext context, BaseFailedFlowEvent failedFlowEvent,
            EnvironmentStatus environmentStatus, ResourceEvent resourceEvent, Enum envState) {
        LOGGER.info("Flow entered into {}", envState.name());
        return environmentService
                .findEnvironmentById(failedFlowEvent.getResourceId())
                .map(environment -> {
                    environment.setStatus(environmentStatus);
                    environment.setStatusReason(failedFlowEvent.getException().getMessage());
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(resourceEvent, simpleResponse, context.getFlowTriggerUserCrn());
                    return environmentDto;
                }).orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot update status of environment, because it does not exist: %s. ", failedFlowEvent.getResourceId())
                ));
    }
}
