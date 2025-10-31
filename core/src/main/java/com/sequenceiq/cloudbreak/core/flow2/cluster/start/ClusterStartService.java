package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DNS_ENTRY_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_FAILED;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterStartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    public void updateDnsEntriesInPem(StackDtoDelegate stack) {
        if (clusterPublicEndpointManagementService.manageCertificateAndDnsInPem(stack.getStack())) {
            String updatingDnsMsg = "Updating the cluster's DNS entries in Public Endpoint Management Service.";
            LOGGER.info(updatingDnsMsg);
            String clusterManagerIp = stackUtil.extractClusterManagerIp(stack);
            clusterPublicEndpointManagementService.refreshDnsEntries(stack);
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_IN_PROGRESS, String.format(updatingDnsMsg + " Cluster manager ip: %s",
                    clusterManagerIp));
            flowMessageService.fireEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_DNS_ENTRY_UPDATE_FINISHED, clusterManagerIp);
        } else {
            LOGGER.debug("Do not update states for updating DNS entries as management in Public Endpoint Management service is disabled.");
        }
    }

    public void startingCluster(StackView stack, ClusterView cluster, InstanceMetadataView primaryGatewayInstance) {
        String clusterManagerIp = stackUtil.extractClusterManagerIp(cluster, primaryGatewayInstance);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_IN_PROGRESS, String.format("Starting the cluster. Cluster manager ip: %s",
                clusterManagerIp));
        flowMessageService.fireEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_STARTING, clusterManagerIp);
    }

    public void clusterStartFinished(StackView stack) {
        clusterService.updatedUpSinceToNowByClusterId(stack.getClusterId());
        updateInstancesToHealthy(stack);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Cluster started.");
        flowMessageService.fireEventAndLog(stack.getId(), Status.AVAILABLE.name(), CLUSTER_STARTED);
    }

    public void handleClusterStartFailure(StackView stackView, String errorReason) {
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.START_FAILED, "Cluster could not be started: " + errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Status.START_FAILED.name(), CLUSTER_START_FAILED, errorReason);
    }

    private void updateInstancesToHealthy(StackView stack) {
        Set<Long> instances = instanceMetaDataService.findNotTerminatedAndNotUnhealthyAndNotZombieIdForStack(stack.getId());
        instanceMetaDataService.updateAllInstancesToStatus(instances, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY,
                "Cluster start finished successfully");
    }
}
