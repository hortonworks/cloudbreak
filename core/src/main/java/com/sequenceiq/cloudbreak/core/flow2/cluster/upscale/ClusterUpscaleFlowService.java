package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
class ClusterUpscaleFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFlowService.class);

    private static final String STATUS_REASON_REPAIR_SINGLE_MASTER = "Repairing single master of cluster.";

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private HostMetadataService hostMetadataService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    void ambariRepairSingleMasterStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SINGLE_MASTER_REPAIR_STARTED, UPDATE_IN_PROGRESS.name());
    }

    void ambariRepairSingleMasterFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SINGLE_MASTER_REPAIR_FINISHED, UPDATE_IN_PROGRESS.name());
    }

    void upscalingClusterManager(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Upscaling the cluster.");
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
    }

    void reRegisterWithClusterProxy(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Re-registering with Cluster Proxy service.");
        flowMessageService.fireEventAndLog(stackId, Msg.RE_REGISTER_WITH_CLUSTER_PROXY, UPDATE_IN_PROGRESS.name());
    }

    void stopClusterManagementServer(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_STOP_MANAGEMENT_SERVER_STARTED, "Stopping cluster management server.");
    }

    void clusterStopComponents(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_STOP_COMPONENTS_STARTED, "Stopping components.");
    }

    void startClusterManagementServer(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_START_MANAGEMENT_SERVER_STARTED, "Starting cluster management server.");
    }

    void regenerateKeytabs(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_REGENERATE_KEYTABS_STARTED, "Regenerating ambari keytabs.");
    }

    void reinstallClusterComponents(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_REINSTALL_COMPONENTS_STARTED, "Reinstalling cluster components.");
    }

    void startComponentsOnNewHosts(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_START_COMPONENTS_STARTED, "Start components on new hosts.");
    }

    void restartAllClusterComponents(long stackId) {
        sendMessage(stackId, Msg.CLUSTER_RESTART_ALL_STARTED, "Restarting all components on all nodes.");
    }

    private void sendMessage(long stackId, Msg ambariMessage, String statusReason) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, ambariMessage, UPDATE_IN_PROGRESS.name());
    }

    void clusterUpscaleFinished(StackView stackView, String hostgroupName, Boolean singlePrimaryGateway) {
        int numOfFailedHosts = updateMetadata(stackView, hostgroupName, singlePrimaryGateway);
        boolean success = numOfFailedHosts == 0;
        if (success) {
            LOGGER.debug("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.CLUSTER_SCALED_UP, AVAILABLE.name());
        } else {
            LOGGER.debug("Cluster upscale failed. {} hosts failed to upscale", numOfFailedHosts);
            clusterService.updateClusterStatusByStackId(stackView.getId(), UPDATE_FAILED);
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added to",
                    String.format("Cluster upscale operation failed on %d node(s).", numOfFailedHosts));
        }
    }

    void clusterUpscaleFailed(long stackId, Exception errorDetails) {
        LOGGER.info("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISIONED,
                String.format("New node(s) could not be added to the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added to", errorDetails);
    }

    private int updateMetadata(StackView stackView, String hostGroupName, Boolean singlePrimaryGateway) {
        LOGGER.debug("Start update metadata");
        HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stackView.getClusterView().getId(), hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        updateFailedHostMetaData(hostMetadata);
        int failedHosts = 0;
        for (HostMetadata hostMeta : hostMetadata) {
            if (hostGroup.getConstraint().getInstanceGroup() != null) {
                stackService.updateMetaDataStatusIfFound(stackView.getId(), hostMeta.getHostName(), InstanceStatus.REGISTERED);
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.getId(), HostMetadataState.HEALTHY);
            if (hostMeta.getHostMetadataState() == HostMetadataState.UNHEALTHY) {
                failedHosts++;
            }
        }
        if (singlePrimaryGateway) {
            instanceMetaDataService
                    .updateUnregisteredInstanceStatusByInstanceGroupForStack(stackView.getId(), InstanceGroupType.CORE, InstanceStatus.REGISTERED);
        }
        return failedHosts;
    }

    private void updateFailedHostMetaData(Collection<HostMetadata> hostMetadata) {
        List<String> upscaleHostNames = getHostNames(hostMetadata);
        Collection<String> successHosts = new HashSet<>(upscaleHostNames);
        updateFailedHostMetaData(successHosts, hostMetadata);
    }

    private void updateFailedHostMetaData(Collection<String> successHosts, Iterable<HostMetadata> hostMetadata) {
        for (HostMetadata metaData : hostMetadata) {
            if (!successHosts.contains(metaData.getHostName())) {
                metaData.setHostMetadataState(HostMetadataState.UNHEALTHY);
                metaData.setStatusReason("Cluster upscale failed. Host failed to upscale.");
                hostMetadataService.save(metaData);
            }
        }
    }

    private List<String> getHostNames(Collection<HostMetadata> hostMetadata) {
        return hostMetadata.stream().map(HostMetadata::getHostName).collect(Collectors.toList());
    }
}
