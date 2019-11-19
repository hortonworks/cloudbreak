package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REGENERATE_KEYTABS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REINSTALL_COMPONENTS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RESTART_ALL_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RE_REGISTER_WITH_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_UP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

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
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    void ambariRepairSingleMasterStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SINGLE_MASTER_REPAIR_STARTED);
    }

    void ambariRepairSingleMasterFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Repairing single master of cluster finished.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SINGLE_MASTER_REPAIR_FINISHED);
    }

    void upscalingClusterManager(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Upscaling the cluster.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_UP);
    }

    void reRegisterWithClusterProxy(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Re-registering with Cluster Proxy service.");
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
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent);
    }

    void clusterUpscaleFinished(StackView stackView, String hostgroupName) {
        int numOfFailedHosts = updateMetadata(stackView, hostgroupName);
        boolean success = numOfFailedHosts == 0;
        if (success) {
            LOGGER.debug("Cluster upscaled successfully");
            clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
            flowMessageService.fireEventAndLog(stackView.getId(), AVAILABLE.name(), CLUSTER_SCALED_UP);
        } else {
            LOGGER.debug("Cluster upscale failed. {} hosts failed to upscale", numOfFailedHosts);
            clusterService.updateClusterStatusByStackId(stackView.getId(), UPDATE_FAILED);
            flowMessageService.fireEventAndLog(stackView.getId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "added to",
                    String.format("Cluster upscale operation failed on %d node(s).", numOfFailedHosts));
        }
    }

    void clusterUpscaleFailed(long stackId, Exception errorDetails) {
        LOGGER.info("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISIONED,
                String.format("New node(s) could not be added to the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "added to", errorDetails.getMessage());
    }

    private int updateMetadata(StackView stackView, String hostGroupName) {
        LOGGER.info("Start update metadata");
        Optional<HostGroup> hostGroupOptional = hostGroupService.getByClusterIdAndName(stackView.getClusterView().getId(), hostGroupName);
        if (hostGroupOptional.isPresent()) {
            Set<InstanceMetaData> notDeletedInstanceMetaDataSet = hostGroupOptional.get().getInstanceGroup().getNotDeletedInstanceMetaDataSet();
            return updateFailedHostMetaData(notDeletedInstanceMetaDataSet);
        } else {
            return 0;
        }
    }

    private int updateFailedHostMetaData(Collection<InstanceMetaData> instanceMetaData) {
        List<String> upscaleHostNames = getHostNames(instanceMetaData);
        Collection<String> successHosts = new HashSet<>(upscaleHostNames);
        return updateFailedHostMetaData(successHosts, instanceMetaData);
    }

    private int updateFailedHostMetaData(Collection<String> successHosts, Iterable<InstanceMetaData> instanceMetaDatas) {
        int failedHosts = 0;
        for (InstanceMetaData metaData : instanceMetaDatas) {
            if (!successHosts.contains(metaData.getDiscoveryFQDN())) {
                instanceMetaDataService.updateInstanceStatus(metaData, InstanceStatus.ORCHESTRATION_FAILED,
                        "Cluster upscale failed. Host does not have fqdn.");
                failedHosts++;
            }
        }
        return failedHosts;
    }

    private List<String> getHostNames(Collection<InstanceMetaData> instanceMetaData) {
        return instanceMetaData.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
    }
}
