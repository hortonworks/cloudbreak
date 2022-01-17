package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FORCE_REMOVING_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REMOVING_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_DOWN;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_DOWN_NONE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_DOWN;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void clusterDownscaleStarted(long stackId, Map<String, Integer> hostGroupWithAdjustment, Map<String, Set<Long>> hostGroupWithPrivateIds,
            ClusterDownscaleDetails details) {
        Set<String> hostGroups = hostGroupWithAdjustment.size() > 0 ? hostGroupWithAdjustment.keySet() : hostGroupWithPrivateIds.keySet();
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_DOWN, String.join(", ", hostGroups));
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        if (hostGroupWithAdjustment.size() > 0) {
            LOGGER.info("Decommissioning hosts '{}'", hostGroupWithAdjustment);
            Integer nodeCount = hostGroupWithAdjustment.values().stream().reduce(0, Integer::sum);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_REMOVING_NODES, String.valueOf(Math.abs(nodeCount)));
        } else {
            LOGGER.info("Decommissioning hosts '{}'", hostGroupWithPrivateIds);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            ResourceEvent resourceEvent = details.isForced() ? CLUSTER_FORCE_REMOVING_NODES : CLUSTER_REMOVING_NODES;
            List<Long> privateIds = hostGroupWithPrivateIds.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            List<String> decommissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), resourceEvent,
                    String.join(", ", decommissionedHostNames));
        }
    }

    public void finalizeClusterScaleDown(Long stackId, @Nullable Set<String> hostGroupName) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        clusterService.updateClusterStatusByStackId(stackView.getId(), DetailedStackStatus.AVAILABLE);
        if (hostGroupName != null) {
            flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALED_DOWN, String.join(", ", hostGroupName));
        } else {
            flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALED_DOWN_NONE);
        }
    }

    public void updateMetadataStatusToFailed(DecommissionResult payload) {
        if (payload.getErrorPhase() != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
            for (String hostName : payload.getHostNames()) {
                instanceMetaDataService.findByHostname(stack.getId(), hostName).ifPresent(instanceMetaData -> {
                    instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.DECOMMISSION_FAILED, payload.getStatusReason());
                });
            }
            String errorDetailes = String.format("The following hosts are '%s': %s", InstanceStatus.DECOMMISSION_FAILED,
                    String.join(", ", payload.getHostNames()));
            flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "removed from", errorDetailes);
        }
    }

    public void updateMetadataStatusToFailed(RemoveHostsFailed payload) {
        for (String hostName : payload.getFailedHostNames()) {
            stackService.updateMetaDataStatusIfFound(payload.getResourceId(), hostName, InstanceStatus.ORCHESTRATION_FAILED,
                    payload.getException().getMessage());
        }
        String errorDetailes = String.format("The following hosts are in '%s': %s",
                InstanceStatus.ORCHESTRATION_FAILED, String.join(", ", payload.getFailedHostNames()));
        flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "removed from", errorDetailes);
    }

    public void handleClusterDownscaleFailure(long stackId, Exception error) {
        String errorDetails = error.getMessage();
        LOGGER.warn("Error during Cluster downscale flow: ", error);
        DetailedStackStatus detailedStackStatus = DetailedStackStatus.DOWNSCALE_FAILED;
        if (error instanceof NotEnoughNodeException) {
            detailedStackStatus = DetailedStackStatus.AVAILABLE;
        }
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, "Node(s) could not be removed from the cluster: " + errorDetails);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "removed from", errorDetails);
    }

}
