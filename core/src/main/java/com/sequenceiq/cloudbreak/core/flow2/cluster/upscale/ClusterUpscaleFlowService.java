package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.UPSCALE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REGENERATE_KEYTABS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REINSTALL_COMPONENTS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RESTART_ALL_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RE_REGISTER_WITH_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_UP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_PARTIALLY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_UP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SINGLE_MASTER_REPAIR_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SINGLE_MASTER_REPAIR_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_COMPONENTS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_MANAGEMENT_SERVER_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_COMPONENTS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_MANAGEMENT_SERVER_STARTED;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
class ClusterUpscaleFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFlowService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    void clusterManagerRepairSingleMasterStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPSCALE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SINGLE_MASTER_REPAIR_STARTED);
    }

    void clusterManagerRepairSingleMasterFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPSCALE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SINGLE_MASTER_REPAIR_FINISHED);
    }

    void upscalingClusterManager(long stackId, Set<String> hostGroups) {
        clusterService.updateClusterStatusByStackId(stackId, UPSCALE_IN_PROGRESS, String.format("Scaling up host group: %s", hostGroups));
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_UP, String.join(", ", hostGroups));
    }

    void reRegisterWithClusterProxy(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPSCALE_IN_PROGRESS, "Re-registering with Cluster Proxy service.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_RE_REGISTER_WITH_CLUSTER_PROXY);
    }

    void stopClusterManagementServer(long stackId) {
        sendMessage(stackId, CLUSTER_STOP_MANAGEMENT_SERVER_STARTED, "Stopping cluster management server.");
    }

    void clusterStopComponents(long stackId) {
        sendMessage(stackId, CLUSTER_STOP_COMPONENTS_STARTED, "Stopping components.");
    }

    void startClusterManagementServer(long stackId) {
        sendMessage(stackId, CLUSTER_START_MANAGEMENT_SERVER_STARTED, "Starting cluster management server.");
    }

    void regenerateKeytabs(long stackId) {
        sendMessage(stackId, CLUSTER_REGENERATE_KEYTABS_STARTED, "Regenerating ambari keytabs.");
    }

    void reinstallClusterComponents(long stackId) {
        sendMessage(stackId, CLUSTER_REINSTALL_COMPONENTS_STARTED, "Reinstalling cluster components.");
    }

    void startComponentsOnNewHosts(long stackId) {
        sendMessage(stackId, CLUSTER_START_COMPONENTS_STARTED, "Start components on new hosts.");
    }

    void restartAllClusterComponents(long stackId) {
        sendMessage(stackId, CLUSTER_RESTART_ALL_STARTED, "Restarting all components on all nodes.");
    }

    private void sendMessage(long stackId, ResourceEvent resourceEvent, String statusReason) {
        clusterService.updateClusterStatusByStackId(stackId, UPSCALE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent);
    }

    void clusterUpscaleFinished(StackView stack, Set<String> hostGroups, boolean repair) {
        int numOfFailedHosts = updateMetadata(stack, hostGroups, repair);
        boolean success = numOfFailedHosts == 0;
        if (success) {
            LOGGER.debug("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.AVAILABLE);
            flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), CLUSTER_SCALED_UP, String.join(", ", hostGroups));
        } else {
            LOGGER.debug("Cluster upscale (partially) failed. {} hosts failed to upscale", numOfFailedHosts);
            clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.UPSCALE_FAILED);
            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_FAILED.name(), CLUSTER_SCALING_PARTIALLY_FAILED, "added to",
                    String.format("Cluster upscale operation failed on %d node(s).", numOfFailedHosts));
        }
    }

    void clusterUpscaleFailed(long stackId, Exception errorDetails) {
        LOGGER.warn("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_FAILED,
                String.format("New node(s) could not be added to the cluster: %s", errorDetails.getMessage()));
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "added to", errorDetails.getMessage());
    }

    private int updateMetadata(StackView stack, Set<String> hostGroups, boolean repair) {
        LOGGER.info("Start update metadata");
        int failedInstances = 0;
        List<InstanceGroupDto> instanceGroups = stackDtoService.getInstanceMetadataByInstanceGroup(stack.getId());
        for (String hostGroup : hostGroups) {
            InstanceGroupDto instanceGroupDto = instanceGroups.stream()
                    .filter(ig -> ig.getInstanceGroup().getGroupName().equals(hostGroup))
                    .findFirst()
                    .orElse(null);
            if (instanceGroupDto != null) {
                List<InstanceMetadataView> notDeletedInstanceMetaDataSet = instanceGroupDto.getNotDeletedAndNotZombieInstanceMetaData();
                failedInstances += updateMissingHostsMetaDatas(notDeletedInstanceMetaDataSet, repair);
            }
        }
        return failedInstances;
    }

    private int updateMissingHostsMetaDatas(Collection<InstanceMetadataView> instanceMetaData, boolean repair) {
        List<String> upscaleHostNames = getHostNames(instanceMetaData);
        Collection<String> successHosts = new HashSet<>(upscaleHostNames);
        if (repair) {
            return updateMissingHostMetaDatas(successHosts, instanceMetaData, InstanceStatus.ORCHESTRATION_FAILED);
        } else {
            return updateMissingHostMetaDatas(successHosts, instanceMetaData, InstanceStatus.ZOMBIE);
        }
    }

    private int updateMissingHostMetaDatas(Collection<String> successHosts, Iterable<InstanceMetadataView> instanceMetaDatas, InstanceStatus instanceStatus) {
        int failedHosts = 0;
        for (InstanceMetadataView metaData : instanceMetaDatas) {
            if (!successHosts.contains(metaData.getDiscoveryFQDN())) {
                instanceMetaDataService.updateInstanceStatus(metaData, instanceStatus, "Cluster upscale failed. Host does not have fqdn.");
                failedHosts++;
            }
        }
        return failedHosts;
    }

    private List<String> getHostNames(Collection<InstanceMetadataView> instanceMetaDatas) {
        return instanceMetaDatas.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toList());
    }
}
