package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_INIT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODE_STOPPING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING2;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sun.istack.Nullable;

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

    public void clusterDownscaleStarted(long stackId, String hostGroupName, Integer scalingAdjustment, Set<Long> privateIds) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_INIT, hostGroupName);
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.DOWNSCALE_BY_STOP_IN_PROGRESS);
        // TODO CB-14929: rationalize instanceIds vs hostnames. stackService.getHostNamesForPrivateIds seems to return instanceIds instead of hostnames.

        // TODO CB-14929: Implement the adjustment where a list of hosts is not provided in the request.
        if (scalingAdjustment != null && scalingAdjustment != 0) {
            LOGGER.info("ZZZ: stopstart scaling Decommissioning {} hosts from host group '{}'", Math.abs(scalingAdjustment), hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                    CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING,
                    String.valueOf(Math.abs(scalingAdjustment)), hostGroupName);
        } else if (!CollectionUtils.isEmpty(privateIds)) {
            LOGGER.info("ZZZ: stopstart scaling Decommissioning {} hosts from host group '{}'", privateIds, hostGroupName);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
            LOGGER.info("ZZZ: Scaling down hostnames: {}", decomissionedHostNames);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                    CLUSTER_SCALING_STOPSTART_DOWNSCALE_STARTING2,
                    String.valueOf(decomissionedHostNames.size()), hostGroupName, String.join(",", decomissionedHostNames));
        }
    }

    public void clusterDownscalingStoppingInstances(long stackId, String hostGroupName, Set<Long> privateIds) {
        LOGGER.info("ZZZ: Attempting to stop nodes (stopstart) {} from host group {}", privateIds, hostGroupName);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
        flowMessageService.fireInstanceGroupEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                CLUSTER_SCALING_STOPSTART_DOWNSCALE_NODE_STOPPING,
                String.valueOf(decomissionedHostNames.size()), hostGroupName, String.join(",", decomissionedHostNames));
    }

    public void clusterDownscaleFinished(Long stackId, @Nullable String hostGroupName, Set<InstanceMetaData> instancesStopped) {
        // TODO CB-14929: Make sure Database state updates are handled correctly.
        instancesStopped.stream().forEach(x -> instanceMetaDataService.updateInstanceStatus(x, InstanceStatus.STOPPED));
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Instances: " + instancesStopped.size() + " stopped successfully.");

        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_FINISHED,
                hostGroupName == null ? "null" : hostGroupName);
    }

    public void handleClusterDownscaleFailure(long stackId, Exception errorDetails) {
        LOGGER.info("Error during stopstart downscale flow: " + errorDetails.getMessage(), errorDetails);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DOWNSCALE_BY_STOP_FAILED,
                String.format("New node(s) (stopstart) could not be removed from the cluster: %s", errorDetails));
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "removed to", errorDetails.getMessage());
    }
}
