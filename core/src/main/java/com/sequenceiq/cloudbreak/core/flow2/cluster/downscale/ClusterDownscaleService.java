package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
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

    public void clusterDownscaleStarted(long stackId, String hostGroupName, Integer scalingAdjustment, Set<Long> privateIds, ClusterDownscaleDetails details) {
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        if (scalingAdjustment != null) {
            LOGGER.debug("Decommissioning {} hosts from host group '{}'", Math.abs(scalingAdjustment), hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stackId, Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, Math.abs(scalingAdjustment), hostGroupName);
        } else if (!CollectionUtils.isEmpty(privateIds)) {
            LOGGER.debug("Decommissioning {} hosts from host group '{}'", privateIds, hostGroupName);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
            Msg message = details.isForced() ? Msg.AMBARI_CLUSTER_FORCE_REMOVING_NODE_FROM_HOSTGROUP : Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP;
            flowMessageService.fireInstanceGroupEventAndLog(stackId, message, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, decomissionedHostNames, hostGroupName);
        }
    }

    public void updateMetadata(Long stackId, Collection<String> hostNames, String hostGroupName) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        ClusterView clusterView = stackView.getClusterView();
        hostNames.forEach(hn -> {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterView.getId(), hostGroupName);
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
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALED_DOWN, AVAILABLE.name());
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
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);

    }
}
