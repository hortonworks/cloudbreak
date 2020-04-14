package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DNS_ENTRY_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_FAILED;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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

    public void updateDnsEntriesInPem(Stack stack) {
        if (clusterPublicEndpointManagementService.manageCertificateAndDnsInPem()) {
            String updatingDnsMsg = "Updating the cluster's DNS entries in Public Endpoint Management Service.";
            LOGGER.info(updatingDnsMsg);
            String clusterManagerIp = stackUtil.extractClusterManagerIp(stack);
            clusterPublicEndpointManagementService.start(stack);
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS);
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, String.format(updatingDnsMsg + " Cluster manager ip: %s",
                    clusterManagerIp));
            flowMessageService.fireEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_DNS_ENTRY_UPDATE_FINISHED, clusterManagerIp);
        } else {
            LOGGER.debug("Do not update states for updating DNS entries as management in Public Endpoint Management service is disabled.");
        }
    }

    public void startingCluster(StackView stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, String.format("Starting the cluster. Cluster manager ip: %s",
                stackUtil.extractClusterManagerIp(stack)));
        flowMessageService.fireEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_STARTING, stackUtil.extractClusterManagerIp(stack));
    }

    public void clusterStartFinished(StackView stack) {
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId())
                .orElseThrow(NotFoundException.notFound("cluster", stack.getId()));
        cluster.setUpSince(new Date().getTime());
        clusterService.updateCluster(cluster);
        updateInstancesToHealthy(stack);
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Cluster started.");
        flowMessageService.fireEventAndLog(stack.getId(), Status.AVAILABLE.name(), CLUSTER_STARTED);
    }

    public void handleClusterStartFailure(StackView stackView, String errorReason) {
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.START_FAILED);
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Cluster could not be started: " + errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Status.START_FAILED.name(), CLUSTER_START_FAILED, errorReason);
    }

    private void updateInstancesToHealthy(StackView stack) {
        Set<InstanceMetaData> instances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
        for (InstanceMetaData metaData : instances) {
            metaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY);
        }
        instanceMetaDataService.saveAll(instances);
    }
}
