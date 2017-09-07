package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
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
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private HostGroupService hostGroupService;

    public void clusterDownscaleStarted(Stack stack, String hostGroupName, Integer scalingAdjustment, Set<String> hostNames) {
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_IN_PROGRESS);
        if (scalingAdjustment != null) {
            LOGGER.info("Decommissioning {} hosts from host group '{}'", scalingAdjustment, hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, scalingAdjustment, hostGroupName);
        } else if (!CollectionUtils.isEmpty(hostNames)) {
            LOGGER.info("Decommissioning {} hosts from host group '{}'", hostNames, hostGroupName);
            flowMessageService.fireInstanceGroupEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP, Status.UPDATE_IN_PROGRESS.name(),
                    hostGroupName, hostNames, hostGroupName);
        }
    }

    public void updateMetadata(Long stackId, Set<String> hostNames, String hostGroupName) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        hostNames.forEach(hn -> {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
            List<HostMetadata> hostMetaToRemove = hostGroup.getHostMetadata().stream()
                    .filter(md -> hostNames.contains(md.getHostName())).collect(Collectors.toList());
            hostGroup.getHostMetadata().removeAll(hostMetaToRemove);
            hostGroupService.save(hostGroup);
        });
        if (!BYOS.equals(stack.cloudPlatform())) {
            LOGGER.info("Start updating metadata");
            for (String hostName : hostNames) {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.DECOMMISSIONED);
            }
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_SCALED_DOWN, AVAILABLE.name());
    }

    public void handleClusterDownscaleFailure(Stack stack, Exception error) {
        String errorDetailes = error.getMessage();
        LOGGER.error("Error during Cluster downscale flow: ", error);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, errorDetailes);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Node(s) could not be removed from the cluster: " + errorDetailes);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", errorDetailes);

    }
}
