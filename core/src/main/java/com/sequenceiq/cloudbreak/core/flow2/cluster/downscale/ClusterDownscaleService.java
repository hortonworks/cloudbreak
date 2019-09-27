package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
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
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private HostMetadataService hostMetadataService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void clusterDownscaleStarted(long stackId, String hostGroupName, Integer scalingAdjustment, Set<Long> privateIds, ClusterDownscaleDetails details) {
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        if (scalingAdjustment != null) {
            LOGGER.debug("Decommissioning {} hosts from host group '{}'", Math.abs(scalingAdjustment), hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Msg.CLUSTER_REMOVING_NODE_FROM_HOSTGROUP, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, Math.abs(scalingAdjustment), hostGroupName);
        } else if (!CollectionUtils.isEmpty(privateIds)) {
            LOGGER.debug("Decommissioning {} hosts from host group '{}'", privateIds, hostGroupName);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
            Msg message = details.isForced() ? Msg.CLUSTER_FORCE_REMOVING_NODE_FROM_HOSTGROUP : Msg.CLUSTER_REMOVING_NODE_FROM_HOSTGROUP;
            flowMessageService.fireInstanceGroupEventAndLog(stackId, message, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, decomissionedHostNames, hostGroupName);
        }
    }

    public void updateMetadata(Long stackId, Collection<String> hostNames, String hostGroupName) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        ClusterView clusterView = stackView.getClusterView();
        hostNames.forEach(hn -> {
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(clusterView.getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            List<HostMetadata> hostMetaToRemove = hostGroup.getHostMetadata().stream()
                    .filter(md -> hostNames.contains(md.getHostName())).collect(Collectors.toList());
            hostGroup.getHostMetadata().removeAll(hostMetaToRemove);
            hostGroupService.save(hostGroup);
        });
        LOGGER.debug("Start updating metadata");
        for (String hostName : hostNames) {
            stackService.updateMetaDataStatusIfFound(stackView.getId(), hostName, InstanceStatus.DECOMMISSIONED);
        }
        clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SCALED_DOWN, AVAILABLE.name());
    }

    public void updateMetadataStatus(DecommissionResult payload) {
        if (payload.getErrorPhase() != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
            InstanceStatus status = getStatus(payload.getErrorPhase());
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
            for (String hostName : payload.getHostNames()) {
                Map<String, Map<String, String>> statusOfComponents = clusterDecomissionService.getStatusOfComponentsForHost(hostName);
                LOGGER.info("State of '{}': {}", hostName, statusOfComponents);
                stackService.updateMetaDataStatusIfFound(payload.getResourceId(), hostName, status);
                hostMetadataService.updateHostMetaDataStatus(stack.getCluster(), hostName, HostMetadataState.UNHEALTHY, payload.getErrorDetails().getMessage());
            }
            String errorDetailes = String.format("The following hosts are in '%s': %s", status, String.join(", ", payload.getHostNames()));
            flowMessageService.fireEventAndLog(payload.getResourceId(),
                    Msg.CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
        }
    }

    public void updateMetadataStatus(RemoveHostsFailed payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
        for (String hostName : payload.getFailedHostNames()) {
            Map<String, Map<String, String>> statusOfComponents = clusterDecomissionService.getStatusOfComponentsForHost(hostName);
            LOGGER.info("State of '{}': {}", hostName, statusOfComponents);
            stackService.updateMetaDataStatusIfFound(payload.getResourceId(), hostName, InstanceStatus.ORCHESTRATION_FAILED);
            hostMetadataService.updateHostMetaDataStatus(stack.getCluster(), hostName, HostMetadataState.UNHEALTHY, payload.getException().getMessage());
        }
        String errorDetailes = String.format("The following hosts are in '%s': %s",
                InstanceStatus.ORCHESTRATION_FAILED, String.join(", ", payload.getFailedHostNames()));
        flowMessageService.fireEventAndLog(payload.getResourceId(),
                Msg.CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
    }

    private InstanceStatus getStatus(String errorPhase) {
        if (errorPhase.equals(DecommissionResult.DECOMMISSION_ERROR_PHASE)) {
            return InstanceStatus.DECOMMISSION_FAILED;
        } else {
            return InstanceStatus.FAILED;
        }
    }

    public void handleClusterDownscaleFailure(long stackId, Exception error) {
        String errorDetailes = error.getMessage();
        LOGGER.debug("Error during Cluster downscale flow: ", error);
        Status status = UPDATE_FAILED;
        if (error instanceof NotEnoughNodeException) {
            status = AVAILABLE;
        }
        clusterService.updateClusterStatusByStackId(stackId, status, errorDetailes);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Node(s) could not be removed from the cluster: " + errorDetailes);
        flowMessageService.fireEventAndLog(stackId, Msg.CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
    }
}
