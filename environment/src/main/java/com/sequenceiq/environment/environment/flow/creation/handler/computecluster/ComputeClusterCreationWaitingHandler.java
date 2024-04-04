package com.sequenceiq.environment.environment.flow.creation.handler.computecluster;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.WAIT_COMPUTE_CLUSTER_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ComputeClusterCreationWaitingHandler extends ExceptionCatcherEventHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeClusterCreationWaitingHandler.class);

    private final ExternalizedComputeService externalizedComputeService;

    private final EnvironmentService environmentService;

    private final PollingService<ComputeClusterPollerObject> computeClusterPollingService;

    protected ComputeClusterCreationWaitingHandler(EventSender eventSender, ExternalizedComputeService externalizedComputeService,
            EnvironmentService environmentService, PollingService<ComputeClusterPollerObject> computeClusterPollingService) {
        this.externalizedComputeService = externalizedComputeService;
        this.environmentService = environmentService;
        this.computeClusterPollingService = computeClusterPollingService;
    }

    @Override
    public String selector() {
        return WAIT_COMPUTE_CLUSTER_CREATION_EVENT.selector();
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
        LOGGER.debug("Compute cluster creation waiting flow step started.");
        String computeClusterName = externalizedComputeService.getComputeClusterDefaultName(environment.getName());
        awaitComputeClusterCreation(environment, computeClusterName);
        LOGGER.debug("Compute cluster creation waiting successfully finished.");
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
    }

    private void awaitComputeClusterCreation(Environment environment, String computeClusterName) {
        ExtendedPollingResult pollWithTimeout = computeClusterPollingService.pollWithTimeout(
                new ComputeClusterCreationRetrievalTask(externalizedComputeService),
                new ComputeClusterPollerObject(environment.getId(), environment.getResourceCrn(), computeClusterName),
                ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_RETRYING_INTERVAL,
                ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_RETRYING_COUNT,
                ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_FAILURE_COUNT);
        if (!pollWithTimeout.isSuccess()) {
            LOGGER.info("Compute cluster creation polling has stopped due to the unsuccessful result: {}", pollWithTimeout.getPollingResult());
            Optional.ofNullable(pollWithTimeout.getException()).ifPresentOrElse(e -> {
                throw new ExternalizedComputeOperationFailedException(e.getMessage());
            }, () -> {
                throw new ExternalizedComputeOperationFailedException("Polling result was: " + pollWithTimeout.getPollingResult());
            });
        }
    }
}
