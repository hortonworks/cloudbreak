package com.sequenceiq.environment.environment.flow.creation.handler.computecluster;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_CONTAINER_ORCHESTRATION_ENGINE_CREATION_STARTED;
import static com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus.LIFTIE_CLUSTER_CREATION_IN_PROGRESS;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

public class ComputeClusterCreationRetrievalTask extends SimpleStatusCheckerTask<ComputeClusterPollerObject> {

    public static final int COMPUTE_CLUSTER_RETRYING_INTERVAL = 25000;

    public static final int COMPUTE_CLUSTER_RETRYING_COUNT = 900;

    public static final int COMPUTE_CLUSTER_FAILURE_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeClusterCreationRetrievalTask.class);

    private final ExternalizedComputeService externalizedComputeService;

    private final EventSenderService eventService;

    private final EnvironmentService environmentService;

    public ComputeClusterCreationRetrievalTask(ExternalizedComputeService externalizedComputeService, EventSenderService eventService,
            EnvironmentService environmentService) {
        this.externalizedComputeService = externalizedComputeService;
        this.eventService = eventService;
        this.environmentService = environmentService;
    }

    @Override
    public boolean checkStatus(ComputeClusterPollerObject pollerObject) {
        String environmentCrn = pollerObject.getEnvironmentCrn();
        String name = pollerObject.getName();

        try {
            LOGGER.debug("Checking the state of compute cluster creation progress: '{}'", name);
            Optional<ExternalizedComputeClusterResponse> response = externalizedComputeService.getComputeCluster(environmentCrn, name);
            if (response.isEmpty()) {
                throw new ExternalizedComputeOperationFailedException(String.format("Compute cluster not found: %s", name));
            }
            ExternalizedComputeClusterResponse computeCluster = response.get();
            ExternalizedComputeClusterApiStatus status = computeCluster.getStatus();
            if (status.isCreationInProgress()) {
                sendNotificationIfNeeded(pollerObject, status, environmentCrn);
                return false;
            } else if (status.isAvailable()) {
                return true;
            } else if (status.isDeletionInProgress() || status.isDeleted()) {
                LOGGER.warn("Compute cluster {} is getting terminated (status:'{}'), polling is cancelled.", name, status);
                throw new ExternalizedComputeOperationFailedException(String.format("Compute cluster %s deleted under the creation process.", name));
            } else if (status.isFailed()) {
                LOGGER.warn("Compute cluster {} is in failed state (status:'{}'), polling is cancelled.", name, status);
                throw new ExternalizedComputeOperationFailedException(String.format("Compute cluster %s failed. Reason: %s",
                        name, computeCluster.getStatusReason()));
            } else {
                LOGGER.warn("Compute cluster {} is in unknown state (status:'{}'), polling is cancelled.", name, status);
                throw new ExternalizedComputeOperationFailedException(String.format("Compute cluster %s failed. Reason: %s",
                        name, computeCluster.getStatusReason()));
            }
        } catch (ExternalizedComputeOperationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalizedComputeOperationFailedException(String.format("Compute cluster %s creation failed. Reason: %s", name, e.getMessage()), e);
        }
    }

    private void sendNotificationIfNeeded(ComputeClusterPollerObject pollerObject, ExternalizedComputeClusterApiStatus status, String environmentCrn) {
        if (!pollerObject.isCreationInProgressNotificationSent() && LIFTIE_CLUSTER_CREATION_IN_PROGRESS.equals(status)) {
            EnvironmentDto environmentDto = environmentService.internalGetByCrn(environmentCrn);
            eventService.sendEventAndNotification(environmentDto, ThreadBasedUserCrnProvider.getUserCrn(),
                    ENVIRONMENT_COMPUTE_CLUSTER_CONTAINER_ORCHESTRATION_ENGINE_CREATION_STARTED);
            pollerObject.creationInProgressNotificationSent();
        }
    }

    @Override
    public void handleTimeout(ComputeClusterPollerObject pollerObject) {
        try {
            Optional<ExternalizedComputeClusterResponse> response = externalizedComputeService.getComputeCluster(pollerObject.getEnvironmentCrn(),
                    pollerObject.getName());
            if (response.isEmpty()) {
                throw new ExternalizedComputeOperationFailedException(
                        String.format("Polling operation timed out, compute cluster %s was not found", pollerObject.getName()));
            }
            throw new ExternalizedComputeOperationFailedException(
                    String.format("Polling operation timed out, compute cluster creation failed. Status: '%s', statusReason: '%s'",
                            response.get().getStatus(), response.get().getStatusReason()));
        } catch (Exception e) {
            throw new ExternalizedComputeOperationFailedException(
                    String.format("Polling operation timed out, compute cluster creation failed and failed to get status: %s", e.getMessage()), e);
        }
    }

    @Override
    public String successMessage(ComputeClusterPollerObject pollerObject) {
        return String.format("Compute cluster creation successfully finished: '%s'", pollerObject.getName());
    }

    @Override
    public boolean exitPolling(ComputeClusterPollerObject pollerObject) {
        PollGroup environmentPollGroup = EnvironmentInMemoryStateStore.get(pollerObject.getEnvironmentId());
        if (environmentPollGroup == null || environmentPollGroup.isCancelled()) {
            LOGGER.info("Cancelling the polling of environment's '{}' compute cluster creation ({}), because a delete operation has already been "
                    + "started on the environment", pollerObject.getEnvironmentCrn(), pollerObject.getName());
            return true;
        }
        return false;
    }
}
