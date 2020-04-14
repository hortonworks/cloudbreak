package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BUILDING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BUILT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RUN_CONTAINERS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RUN_SERVICES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_GATEWAY_CERTIFICATE_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_BOOTSTRAP;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.OrchestratorView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
public class ClusterCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private TransactionService transactionService;

    public void bootstrappingMachines(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.BOOTSTRAPPING_MACHINES);
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_BOOTSTRAP);
    }

    public void registeringToClusterProxy(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REGISTERING_TO_CLUSTER_PROXY);
    }

    public void registeringGatewayToClusterProxy(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REGISTERING_GATEWAY_TO_CLUSTER_PROXY);
    }

    public void collectingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.COLLECTING_HOST_METADATA);
    }

    public void bootstrapPublicEndpoints(Stack stack) {
        boolean success = clusterPublicEndpointManagementService.provision(stack);
        if (!success) {
            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_GATEWAY_CERTIFICATE_CREATE_FAILED);
        }
    }

    public void startingClusterServices(StackView stack) throws CloudbreakException {
        OrchestratorView orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTING_CLUSTER_MANAGER_SERVICES, "Running cluster services.");
        if (orchestratorType.containerOrchestrator()) {
            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RUN_CONTAINERS);
        } else if (orchestratorType.hostOrchestrator()) {
            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RUN_SERVICES);
        } else {
            String message = String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType());
            LOGGER.error(message);
            throw new CloudbreakException(message);
        }
    }

    public void startingClusterManager(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "cluster manager cluster is now starting.");
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
    }

    public void installingCluster(StackView stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION,
                String.format("Building the cluster"));
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_BUILDING);
    }

    public void clusterInstallationFinished(StackView stackView) {
        try {
            transactionService.required(() -> {
                clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
                stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Cluster creation finished.");
                flowMessageService.fireEventAndLog(stackView.getId(), AVAILABLE.name(), CLUSTER_BUILT);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void handleClusterCreationFailure(StackView stackView, Exception exception) {
        if (stackView.getClusterView() != null) {
            Cluster cluster = clusterService.getById(stackView.getClusterView().getId());
            String errorMessage = getErrorMessageFromException(exception);
            clusterService.updateClusterStatusByStackId(stackView.getId(), CREATE_FAILED, errorMessage);
            stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE);
            flowMessageService.fireEventAndLog(stackView.getId(), CREATE_FAILED.name(), CLUSTER_CREATE_FAILED, errorMessage);
            try {
                OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stackView.getOrchestrator().getType());
                if (cluster != null && orchestratorType.containerOrchestrator()) {
                    clusterTerminationService.deleteClusterContainers(cluster);
                }
            } catch (CloudbreakException | TerminationFailedException ex) {
                LOGGER.info("Cluster containers could not be deleted, preparation for reinstall failed: ", ex);
            }
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }

    private String getErrorMessageFromException(Exception exception) {
        if (exception instanceof TransactionRuntimeExecutionException && exception.getCause() != null && exception.getCause().getCause() != null) {
            return exception.getCause().getCause().getMessage();
        } else {
            return exception instanceof CloudbreakException && exception.getCause() != null
                    ? exception.getCause().getMessage() : exception.getMessage();
        }
    }
}
