package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FORCE_REMOVING_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REMOVING_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_REMOVING_ZOMBIE_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_DOWN;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALED_DOWN_NONE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_DOWN;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_DOWN_ZOMBIE_NODES;
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
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.service.NodeIsBusyException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public CollectDownscaleCandidatesRequest clusterDownscaleStarted(long stackId, ClusterDownscaleTriggerEvent payload) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        ClusterDownscaleDetails details = payload.getDetails();
        if (details != null && details.isPurgeZombies()) {
            return collectZombieNodesForDownscale(stackId, payload);
        }
        Map<String, Integer> hostGroupsWithAdjustment = payload.getHostGroupsWithAdjustment();
        Map<String, Set<Long>> hostGroupsWithPrivateIds = payload.getHostGroupsWithPrivateIds();
        Set<String> hostGroups = hostGroupsWithAdjustment.size() > 0 ? hostGroupsWithAdjustment.keySet() : hostGroupsWithPrivateIds.keySet();
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_DOWN, String.join(", ", hostGroups));
        if (!CollectionUtils.isEmpty(hostGroupsWithAdjustment)) {
            LOGGER.info("Decommissioning hosts '{}'", hostGroupsWithAdjustment);
            Integer nodeCount = hostGroupsWithAdjustment.values().stream().reduce(0, Integer::sum);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_REMOVING_NODES, String.valueOf(Math.abs(nodeCount)));
        } else {
            LOGGER.info("Decommissioning hosts '{}'", hostGroupsWithPrivateIds);
            ResourceEvent resourceEvent = (details != null && details.isForced()) ? CLUSTER_FORCE_REMOVING_NODES : CLUSTER_REMOVING_NODES;
            List<Long> privateIds = hostGroupsWithPrivateIds.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            List<String> decommissionedHostNames = instanceMetaDataService.getAllAvailableHostNamesByPrivateIds(stackId, privateIds);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), resourceEvent,
                    String.join(", ", decommissionedHostNames));
        }
        return new CollectDownscaleCandidatesRequest(stackId, hostGroupsWithAdjustment, hostGroupsWithPrivateIds, details);
    }

    private CollectDownscaleCandidatesRequest collectZombieNodesForDownscale(long stackId, ClusterDownscaleTriggerEvent payload) {
        Set<String> zombieHostGroups = payload.getZombieHostGroups();
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_DOWN_ZOMBIE_NODES,
                String.join(", ", zombieHostGroups));
        StackDto stack = stackDtoService.getById(stackId);
        Map<String, Set<Long>> zombiePrivateIdsByHostGroups = stack.getZombieInstanceMetaData().stream()
                .filter(instanceMetaData -> zombieHostGroups.contains(instanceMetaData.getInstanceGroupName()))
                .collect(Collectors.groupingBy(
                        InstanceMetadataView::getInstanceGroupName,
                        Collectors.mapping(InstanceMetadataView::getPrivateId, Collectors.toSet())));
        LOGGER.info("Decommissioning ZOMBIE nodes '{}'", zombiePrivateIdsByHostGroups);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_REMOVING_ZOMBIE_NODES,
                String.valueOf(zombiePrivateIdsByHostGroups.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()).size()));
        return new CollectDownscaleCandidatesRequest(stackId, payload.getHostGroupsWithAdjustment(),
                zombiePrivateIdsByHostGroups, payload.getDetails());
    }

    public void finalizeClusterScaleDown(Long stackId, @Nullable Set<String> hostGroupName) {
        StackView stackView = stackDtoService.getStackViewById(stackId);
        clusterService.updateClusterStatusByStackId(stackView.getId(), DetailedStackStatus.AVAILABLE);
        if (hostGroupName != null) {
            flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALED_DOWN, String.join(", ", hostGroupName));
        } else {
            flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_SCALED_DOWN_NONE);
        }
    }

    public void updateMetadataStatusToFailed(DecommissionResult payload) {
        if (payload.getErrorPhase() != null) {
            StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
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
        if (error instanceof NotEnoughNodeException || error instanceof NodeIsBusyException) {
            detailedStackStatus = DetailedStackStatus.AVAILABLE;
        }
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, "Node(s) could not be removed from the cluster: " + errorDetails);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, "removed from", errorDetails);
    }

}
