package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
public class ClusterCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationService.class);

    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;
    @Inject
    private ClusterService clusterService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private ClusterTerminationService clusterTerminationService;

    public void bootstrappingMachines(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_BOOTSTRAP, UPDATE_IN_PROGRESS.name());
    }

    public void collectingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_METADATA_SETUP, UPDATE_IN_PROGRESS.name());
    }

    public void startingAmbariServices(Stack stack, Cluster cluster) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Running cluster services.");
        if (orchestratorType.containerOrchestrator()) {
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_RUN_CONTAINERS, UPDATE_IN_PROGRESS.name());
        } else if (orchestratorType.hostOrchestrator()) {
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_RUN_SERVICES, UPDATE_IN_PROGRESS.name());
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
    }

    public void startingAmbari(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Ambari cluster is now starting.");
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
    }

    public void installingCluster(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, String.format("Building the Ambari cluster. Ambari ip:%s", stack.getAmbariIp()));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_BUILDING, UPDATE_IN_PROGRESS.name(), stack.getAmbariIp());
    }

    public void clusterInstallationFinished(Stack stack, Cluster cluster) {
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster creation finished.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_BUILT, AVAILABLE.name(), stack.getAmbariIp());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningSuccessEmail(cluster.getOwner(), stack.getCluster().getEmailTo(), stack.getAmbariIp(), cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
    }

    public void handleClusterCreationFailure(Stack stack, Exception exception) {
        Cluster cluster = clusterService.getById(stack.getCluster().getId());
        clusterService.updateClusterStatusByStackId(stack.getId(), CREATE_FAILED, exception.getMessage());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_CREATE_FAILED, CREATE_FAILED.name(), exception.getMessage());

        // TODO Only triggering the deleteClusterContainers flow
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (cluster != null && orchestratorType.containerOrchestrator()) {
                clusterTerminationService.deleteClusterContainers(cluster);
            }
        } catch (CloudbreakException | TerminationFailedException ex) {
            LOGGER.error("Cluster containers could not be deleted, preparation for reinstall failed: ", ex);
        }

        if (cluster != null && cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningFailureEmail(cluster.getOwner(), stack.getCluster().getEmailTo(), cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
    }
}
