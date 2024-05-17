package com.sequenceiq.environment.environment.flow.externalizedcluster.create.handler;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationHandlerSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_WAIT_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExternalizedComputeClusterCreationWaitHandler extends ExceptionCatcherEventHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreationWaitHandler.class);

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public String selector() {
        return DEFAULT_COMPUTE_CLUSTER_CREATION_WAIT_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentDto> event) {
        return new ExternalizedComputeClusterCreationFailedEvent(event.getData(), e, EnvironmentStatus.AVAILABLE);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentDto> event) {
        EnvironmentDto environmentDto = event.getData();
        Environment environment = environmentService.findEnvironmentByIdOrThrow(environmentDto.getId());
        LOGGER.debug("Compute cluster creation waiting flow step started.");
        String computeClusterName = externalizedComputeService.getDefaultComputeClusterName(environment.getName());
        externalizedComputeService.awaitComputeClusterCreation(environment, computeClusterName);
        LOGGER.debug("Compute cluster creation waiting successfully finished.");
        return ExternalizedComputeClusterCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
    }
}
