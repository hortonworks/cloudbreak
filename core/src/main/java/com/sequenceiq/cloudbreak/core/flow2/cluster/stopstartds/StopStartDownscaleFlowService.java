package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_COULDNOTDECOMMISSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_INIT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODES_NOT_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODES_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODE_STOPPING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING_IDENTIFIEDRECOVERYCANDIDATES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_STOP_FAILED;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class StopStartDownscaleFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleFlowService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void initScaleDown(long stackId, String hostGroupName) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.DOWNSCALE_BY_STOP_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_INIT, hostGroupName);
    }

    public void clusterDownscaleStarted(long stackId, String hostGroupName, Set<Long> privateIds, Set<String> recoveryCandidateHostIds) {
        // TODO CB-15153: Change the message once an adjustment based downscale is supported.
        LOGGER.debug("stopstart scaling Decommissioning from group: {}, privateIds: [{}]", hostGroupName, privateIds);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getTerminatedAndNonTerminatedInstanceMetaDataAsList(), privateIds);
        List<String> recoveryCandidateHostNames;
        if (!recoveryCandidateHostIds.isEmpty()) {
            recoveryCandidateHostNames = stack.getAllAvailableInstances().stream().filter(i -> recoveryCandidateHostIds.contains(i.getInstanceId()))
                    .map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toList());
            LOGGER.debug("stop start scaling Decommissioning from group: {}, hostnames: [{}], identified recovery candidates: [{}]",
                    hostGroupName, decomissionedHostNames, recoveryCandidateHostNames);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                    CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING_IDENTIFIEDRECOVERYCANDIDATES,
                    String.valueOf(decomissionedHostNames.size()), hostGroupName, String.join(", ", decomissionedHostNames),
                    String.valueOf(recoveryCandidateHostIds.size()),
                    String.join(", ", recoveryCandidateHostNames));
        } else {
            LOGGER.debug("stopstart scaling Decommissioning from group: {}, hostnames: [{}]", hostGroupName, decomissionedHostNames);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                    CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING,
                    String.valueOf(decomissionedHostNames.size()), hostGroupName, String.join(", ", decomissionedHostNames));

        }
    }

    public void logCouldNotDecommission(long stackId, List<String> notDecommissionedFqdns) {
        // TODO CB-14929: CB-15418 This needs to be an orange message (i.e. not success, not failure). Need to figure out how the UI
        //  processes these and applies icons.
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_COULDNOTDECOMMISSION,
                String.valueOf(notDecommissionedFqdns.size()), String.join(", ", notDecommissionedFqdns));
    }

    public void clusterDownscalingStoppingInstances(long stackId, String hostGroupName, Set<String> decommissionedFqdns) {
        flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODE_STOPPING,
                String.valueOf(decommissionedFqdns.size()), hostGroupName, String.join(", ", decommissionedFqdns));
    }

    public void instancesStopped(long stackId, List<InstanceMetadataView> instancesStopped) {
        List<Long> instanceIds = instancesStopped.stream().map(im -> im.getId()).collect(Collectors.toList());
        instanceMetaDataService.updateAllInstancesToStatus(instanceIds, InstanceStatus.STOPPED, "Instance successfully stopped");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODES_STOPPED,
                String.valueOf(instancesStopped.size()), instancesStopped.stream().map(InstanceMetadataView::getInstanceId)
                        .collect(Collectors.joining(", ")));
    }

    public void logInstancesFailedToStop(long stackId, List<CloudVmInstanceStatus> notStoppedInstances) {
        // TODO CB-14929: CB-15418 This needs to be an orange message (i.e. not success, not failure). Need to figure out how the UI
        //  processes these and applies icons.
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODES_NOT_STOPPED,
                String.valueOf(notStoppedInstances.size()),
                        notStoppedInstances.stream().map(x -> x.getCloudInstance().getInstanceId()).collect(Collectors.joining(", ")));
    }

    public void clusterDownscaleFinished(Long stackId, String hostGroupName, List<InstanceMetadataView> instancesStopped) {
        stackUpdater.updateStackStatus(
                stackId,
                DetailedStackStatus.AVAILABLE,
                "Instances: " + instancesStopped.size() + " stopped successfully.");
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_FINISHED,
                hostGroupName, String.valueOf(instancesStopped.size()),
                instancesStopped.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.joining(", ")));
    }

    public void decommissionViaCmFailed(long stackId, Set<String> hostnamesAttempted) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_DECOMMISSION_FAILED,
                String.valueOf(hostnamesAttempted.size()), String.join(", ", hostnamesAttempted));
    }

    public void stopInstancesFailed(long stackId, List<CloudInstance> attemptedStopInstances) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_STOP_FAILED,
                String.valueOf(attemptedStopInstances.size()),
                attemptedStopInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(", ")));
    }

    public void handleClusterDownscaleFailure(long stackId, Exception errorDetails) {
        LOGGER.info("Error during stopstart downscale flow: " + errorDetails.getMessage(), errorDetails);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DOWNSCALE_BY_STOP_FAILED,
                String.format("New node(s) (stopstart) could not be removed from the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_FAILED, errorDetails.getMessage());
    }
}
