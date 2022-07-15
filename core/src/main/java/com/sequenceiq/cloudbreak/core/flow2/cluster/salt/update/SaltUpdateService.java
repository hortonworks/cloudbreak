package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SALT_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RUN_SERVICES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_PASSWORD_ROTATE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_UPDATE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_BOOTSTRAP;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class SaltUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterCreationService clusterCreationService;

    public void bootstrappingMachines(Long stackId) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SALT_UPDATE_STARTED);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.BOOTSTRAPPING_MACHINES);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_BOOTSTRAP);
    }

    public void startingClusterServices(Long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.STARTING_CLUSTER_MANAGER_SERVICES, "Running cluster services.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_RUN_SERVICES);
    }

    public void rotateSaltPassword(Long stackId) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SALT_PASSWORD_ROTATE_STARTED);
    }

    public void clusterInstallationFinished(Long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Salt update finished.");
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SALT_UPDATE_FINISHED);
    }

    public void handleClusterCreationFailure(StackView stackView, Exception exception) {
        if (stackView.getClusterId() != null) {
            String errorMessage = clusterCreationService.getErrorMessageFromException(exception);
            stackUpdater.updateStackStatus(stackView.getId(), SALT_UPDATE_FAILED, errorMessage);
            flowMessageService.fireEventAndLog(stackView.getId(), UPDATE_FAILED.name(), CLUSTER_SALT_UPDATE_FAILED, errorMessage);
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }
}
