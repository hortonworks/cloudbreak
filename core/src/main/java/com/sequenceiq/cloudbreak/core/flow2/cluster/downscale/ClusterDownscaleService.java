package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.NotEnoughNodeException;
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

    public void clusterDownscaleStarted(long stackId, String hostGroupName, Integer scalingAdjustment, Set<Long> privateIds, ClusterDownscaleDetails details) {
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        if (scalingAdjustment != null) {
            LOGGER.info("Decommissioning {} hosts from host group '{}'", Math.abs(scalingAdjustment), hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, Math.abs(scalingAdjustment), hostGroupName);
        } else if (!CollectionUtils.isEmpty(privateIds)) {
            LOGGER.info("Decommissioning {} hosts from host group '{}'", privateIds, hostGroupName);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
            Msg message = details.isForced() ? Msg.AMBARI_CLUSTER_FORCE_REMOVING_NODE_FROM_HOSTGROUP : Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP;
            flowMessageService.fireInstanceGroupEventAndLog(stackId, message, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, decomissionedHostNames, hostGroupName);
        }
    }

    public void finalizeClusterScaleDown(Long stackId) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        clusterService.updateClusterStatusByStackId(stackView.getId(), AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALED_DOWN, AVAILABLE.name());
    }

    public void updateMetadataStatus(DecommissionResult payload) {
        if (payload.getErrorPhase() != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getStackId());
            for (String hostName : payload.getHostNames()) {
                InstanceMetaData instanceMetaData = instanceMetaDataService.findByHostname(stack.getId(), hostName);
                instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.SERVICES_UNHEALTHY, payload.getStatusReason());
            }
            String errorDetailes = String.format("The following hosts are '%s': %s", InstanceStatus.SERVICES_UNHEALTHY,
                    String.join(", ", payload.getHostNames()));
            flowMessageService.fireEventAndLog(payload.getStackId(),
                    Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
        }
    }

    public void updateMetadataStatus(RemoveHostsFailed payload) {
        for (String hostName : payload.getFailedHostNames()) {
            stackService.updateMetaDataStatusIfFound(payload.getStackId(), hostName, InstanceStatus.ORCHESTRATION_FAILED, payload.getException().getMessage());
        }
        String errorDetailes = String.format("The following hosts are in '%s': %s",
                InstanceStatus.ORCHESTRATION_FAILED, String.join(", ", payload.getFailedHostNames()));
        flowMessageService.fireEventAndLog(payload.getStackId(),
                Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
    }

    public void handleClusterDownscaleFailure(long stackId, Exception error) {
        String errorDetailes = error.getMessage();
        LOGGER.warn("Error during Cluster downscale flow: ", error);
        Status status = UPDATE_FAILED;
        if (error instanceof NotEnoughNodeException) {
            status = AVAILABLE;
        }
        clusterService.updateClusterStatusByStackId(stackId, status, errorDetailes);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Node(s) could not be removed from the cluster: " + errorDetailes);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);
    }
}
