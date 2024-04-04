package com.sequenceiq.environment.environment.flow.creation.handler.computecluster;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_COMPUTE_CLUSTER_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ComputeClusterCreationHandler extends ExceptionCatcherEventHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeClusterCreationHandler.class);

    private final ExternalizedComputeService externalizedComputeService;

    private final EnvironmentService environmentService;

    protected ComputeClusterCreationHandler(ExternalizedComputeService externalizedComputeService, EnvironmentService environmentService) {
        this.externalizedComputeService = externalizedComputeService;
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return CREATE_COMPUTE_CLUSTER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentDto> event) {
        EnvironmentDto environmentDto = event.getData();
        return new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentDto> event) {
        EnvironmentDto environmentDto = event.getData();
        Environment environment = environmentService.findEnvironmentByIdOrThrow(environmentDto.getId());
        LOGGER.debug("Compute cluster creation flow step started.");
        externalizedComputeService.createComputeCluster(environment);
        LOGGER.debug("Compute cluster creation started.");
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
    }
}
