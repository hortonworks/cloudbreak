package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClusterStatusUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusUpdater.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void updateClusterStatus(Stack stack, Cluster cluster) {
        Long stackId = stack.getId();
        if (isStackOrClusterStatusInvalid(stack, cluster)) {
            List<String> eventMessageArgs = Arrays.asList(stack.getStatus().name());
            cloudbreakEventService.fireCloudbreakEvent(stackId, stack.getStatus().name(), CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC, eventMessageArgs);
        } else if (cluster != null && cluster.getClusterManagerIp() != null) {
            clusterService.updateClusterMetadata(stackId);
            String blueprintName = cluster.getBlueprint().getStackName();
            ClusterStatusResult clusterStatusResult =
                    clusterApiConnectors.getConnector(stack).clusterStatusService().getStatus(StringUtils.isNotBlank(blueprintName));
            LOGGER.debug("Cluster status: [{}] Status reason: [{}]", clusterStatusResult.getClusterStatus(), clusterStatusResult.getStatusReason());
            updateClusterStatus(stack, clusterStatusResult);
        }
    }

    private boolean isStackOrClusterStatusInvalid(Stack stack, Cluster cluster) {
        return stack.isStackInDeletionPhase()
                || stack.isStackInStopPhase()
                || stack.isModificationInProgress()
                || cluster == null;
    }

    private void updateClusterStatus(Stack stack, ClusterStatusResult clusterStatusResult) {
        Status statusInEvent = stack.getStatus();
        ClusterStatus clusterStatus = clusterStatusResult.getClusterStatus();
        String statusReason = clusterStatusResult.getStatusReason();
        if (isUpdateEnabled(clusterStatus)) {
            if (updateClusterStatus(stack, clusterStatus)) {
                statusInEvent = clusterStatus.getDetailedStackStatus().getStatus();
                statusReason = clusterStatus.getStatusReason();
            } else {
                statusReason = "The cluster's state is up to date.";
            }
        }
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), statusInEvent.name(), CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                Collections.singletonList(statusReason));
    }

    private boolean isUpdateEnabled(ClusterStatus clusterStatus) {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED || clusterStatus == ClusterStatus.AMBIGUOUS;
    }

    private boolean updateClusterStatus(Stack stack, ClusterStatus clusterStatus) {
        boolean result = false;

        Cluster cluster = stack.getCluster();
        Status actualStatus = stack.getStatus();
        DetailedStackStatus newDetailedStackStatus = clusterStatus.getDetailedStackStatus();
        Status newStatus = newDetailedStackStatus.getStatus();
        if (actualStatus != newStatus) {
            if (!actualStatus.equals(Status.MAINTENANCE_MODE_ENABLED) || !newStatus.equals(Status.AVAILABLE)) {
                LOGGER.debug("Cluster {} status is updated from {} to {}/{}", cluster.getId(), actualStatus, newStatus, newDetailedStackStatus);
                clusterService.updateClusterStatusByStackId(stack.getId(), newDetailedStackStatus, clusterStatus.getStatusReason());
                result = true;
            }
        } else {
            LOGGER.debug("Cluster {} status hasn't changed: {}", cluster.getId(), actualStatus);
        }
        return result;
    }
}
