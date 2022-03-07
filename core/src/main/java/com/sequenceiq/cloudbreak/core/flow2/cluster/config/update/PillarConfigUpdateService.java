package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PILLAR_CONFIG_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PILLAR_CONFIG_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PILLAR_CONFIG_UPDATE_STARTED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class PillarConfigUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillarConfigUpdateService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterCreationService clusterCreationService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterService clusterService;

    public void doConfigUpdate(Long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.BOOTSTRAPPING_MACHINES);
        flowMessageService
            .fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(),
                CLUSTER_PILLAR_CONFIG_UPDATE_STARTED);
        Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
        Long clusterId = stack.getCluster().getId();
        Cluster cluster = clusterService.findOneWithLists(clusterId).orElseThrow(NotFoundException.notFound("Cluster", clusterId));
        clusterHostServiceRunner.updateClusterConfigs(stack, cluster);
    }

    public void configUpdateFinished(StackView stackView) {
        try {
            transactionService.required(() -> {
                stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Config update finished.");
            });
            flowMessageService.fireEventAndLog(stackView.getId(), AVAILABLE.name(),
                CLUSTER_PILLAR_CONFIG_UPDATE_FINISHED);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void handleConfigUpdateFailure(StackView stackView, Exception exception) {
        try {
            if (stackView.getClusterView() != null) {
                String errorMessage = clusterCreationService
                    .getErrorMessageFromException(exception);
                transactionService.required(() -> {
                    stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.PILLAR_CONFIG_UPDATE_FAILED, errorMessage);
                });
                flowMessageService.fireEventAndLog(stackView.getId(), UPDATE_FAILED.name(),
                    CLUSTER_PILLAR_CONFIG_UPDATE_FAILED, errorMessage);
            } else {
                LOGGER.info("Cluster was null. Flow action was not required.");
            }
        } catch (TransactionExecutionException e) {
            LOGGER.warn("Error setting status for Pillar configuration update failure", e);
        }
    }
}
