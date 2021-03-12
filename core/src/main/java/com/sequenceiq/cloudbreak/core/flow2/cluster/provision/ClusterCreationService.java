package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BUILDING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BUILT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RUN_SERVICES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_GATEWAY_CERTIFICATE_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_BOOTSTRAP;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;

@Component
public class ClusterCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

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

    public void bootstrapPrivateEndpoints(Stack stack) {
        clusterPublicEndpointManagementService.registerLoadBalancerWithFreeIPA(stack);
    }

    public void startingClusterServices(StackView stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTING_CLUSTER_MANAGER_SERVICES, "Running cluster services.");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RUN_SERVICES);
    }

    public void installingCluster(StackView stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, "Building the cluster");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_BUILDING);
    }

    public void startingClusterManager(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "cluster manager cluster is now starting.");
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
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
            String errorMessage = getErrorMessageFromException(exception);
            clusterService.updateClusterStatusByStackId(stackView.getId(), CREATE_FAILED, errorMessage);
            stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE);
            flowMessageService.fireEventAndLog(stackView.getId(), CREATE_FAILED.name(), CLUSTER_CREATE_FAILED, errorMessage);
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }

    public String getErrorMessageFromException(Exception exception) {
        if (exception instanceof TransactionRuntimeExecutionException && exception.getCause() != null && exception.getCause().getCause() != null) {
            return exception.getCause().getCause().getMessage();
        } else {
            return exception instanceof CloudbreakException && exception.getCause() != null
                    ? exception.getCause().getMessage() : exception.getMessage();
        }
    }
}
