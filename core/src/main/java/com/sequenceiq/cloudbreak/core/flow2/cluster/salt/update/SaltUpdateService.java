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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Component
public class SaltUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterCreationService clusterCreationService;

    public void bootstrappingMachines(Stack stack) {
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SALT_UPDATE_STARTED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.BOOTSTRAPPING_MACHINES);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_BOOTSTRAP);
    }

    public void startingClusterServices(StackView stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTING_CLUSTER_MANAGER_SERVICES, "Running cluster services.");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RUN_SERVICES);
    }

    public void rotateSaltPassword(Stack stack) {
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SALT_PASSWORD_ROTATE_STARTED);
    }

    public void clusterInstallationFinished(StackView stackView) {
        try {
            transactionService.required(() -> {
                stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Salt update finished.");
                flowMessageService.fireEventAndLog(stackView.getId(), AVAILABLE.name(), CLUSTER_SALT_UPDATE_FINISHED);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void handleClusterCreationFailure(StackView stackView, Exception exception) {
        if (stackView.getClusterView() != null) {
            String errorMessage = clusterCreationService.getErrorMessageFromException(exception);
            stackUpdater.updateStackStatus(stackView.getId(), SALT_UPDATE_FAILED, errorMessage);
            flowMessageService.fireEventAndLog(stackView.getId(), UPDATE_FAILED.name(), CLUSTER_SALT_UPDATE_FAILED, errorMessage);
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }
}
