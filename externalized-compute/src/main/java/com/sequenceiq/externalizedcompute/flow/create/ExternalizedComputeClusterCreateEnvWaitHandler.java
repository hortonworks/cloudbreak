package com.sequenceiq.externalizedcompute.flow.create;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExternalizedComputeClusterCreateEnvWaitHandler extends ExceptionCatcherEventHandler<ExternalizedComputeClusterCreateEnvWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateEnvWaitHandler.class);

    @Value("${externalizedcompute.create.env.wait.sleep.time.sec:10}")
    private int sleepTime;

    @Value("${externalizedcompute.create.env.wait.sleep.time.limit.hour:2}")
    private int timeLimit;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExternalizedComputeClusterCreateEnvWaitRequest> event) {
        return new ExternalizedComputeClusterCreateFailedEvent(resourceId, event.getData().getActorCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExternalizedComputeClusterCreateEnvWaitRequest> event) {
        Long resourceId = event.getData().getResourceId();
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(resourceId);
        String actorCrn = event.getData().getActorCrn();
        String environmentCrn = externalizedComputeCluster.getEnvironmentCrn();
        try {
            return Polling.stopAfterDelay(timeLimit, TimeUnit.HOURS)
                    .stopIfException(false)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(() -> {
                        try {
                            DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(environmentCrn);
                            EnvironmentStatus environmentStatus = environment.getEnvironmentStatus();
                            LOGGER.debug("Environment status: {}", environmentStatus);
                            if (environmentStatus == null) {
                                LOGGER.error("Environment status is null");
                                return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn,
                                        new RuntimeException("Environment status is null")));
                            } else if (environmentStatus.isComputeClusterCreationInProgress() || environmentStatus.isAvailable()) {
                                LOGGER.info("Environment is in COMPUTE_CLUSTER_CREATION_IN_PROGRESS or AVAILABLE state, proceed to compute cluster creation.");
                                return AttemptResults.finishWith(
                                        new ExternalizedComputeClusterCreateEnvWaitSuccessResponse(resourceId, actorCrn));
                            } else if (environmentStatus.isFailed()) {
                                LOGGER.error("Environment is in failed status: {}", environmentStatus);
                                return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn,
                                        new RuntimeException(String.format("Environment creation failed. Status: %s. Message: %s", environmentStatus,
                                                environmentStatus.getDescription()))));
                            } else if (environmentStatus.isDeleteInProgress()) {
                                return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn,
                                        new RuntimeException(String.format("Environment is deleted. Status: %s. Message: %s", environmentStatus,
                                                environmentStatus.getDescription()))));
                            } else {
                                LOGGER.debug("Environment is in {}, waiting for COMPUTE_CLUSTER_CREATION_IN_PROGRESS state", environmentStatus);
                                return AttemptResults.justContinue();
                            }
                        } catch (NotFoundException notFoundException) {
                            LOGGER.warn("Environment not found: {}", environmentCrn);
                            return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn,
                                    new RuntimeException("Environment not found")));
                        }
                    });
        } catch (PollerStoppedException e) {
            DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(environmentCrn);
            LOGGER.warn("Environment wait operation timed out. Env response: {}", environment,  e);
            return new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn, new RuntimeException("Environment wait operation timed out" +
                    "The last known status is:" + environment.getEnvironmentStatus()));
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterCreateEnvWaitRequest.class);
    }
}

