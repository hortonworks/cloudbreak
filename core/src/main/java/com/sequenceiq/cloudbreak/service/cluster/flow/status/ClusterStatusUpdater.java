package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

@Component
public class ClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusUpdater.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void updateClusterStatus(Stack stack, Cluster cluster) {
        if (isStackOrClusterStatusInvalid(stack, cluster)) {
            if ((stack.isStackInStopPhase() || stack.isDeleteCompleted()) && cluster != null) {
                updateClusterStatus(stack.getId(), cluster, stack.getStatus());
                cluster.setStatus(stack.getStatus());
            }
            String msg = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_COULD_NOT_SYNC.code(), Arrays.asList(stack.getStatus(),
                    cluster == null ? "" : cluster.getStatus()));
            LOGGER.warn(msg);
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), msg);
        } else if (cluster != null && cluster.getAmbariIp() != null) {
            Long stackId = stack.getId();
            clusterService.updateClusterMetadata(stackId);
            String blueprintName = cluster.getBlueprint().getStackName();
            ClusterStatusResult clusterStatusResult =
                    clusterApiConnectors.getConnector(stack).clusterStatusService().getStatus(StringUtils.isNotBlank(blueprintName));
            LOGGER.debug("Ambari cluster status: [{}] Status reason: [{}]", clusterStatusResult.getClusterStatus(), clusterStatusResult.getStatusReason());
            updateClusterStatus(stackId, stack.getStatus(), cluster, clusterStatusResult);
        }
    }

    private boolean isStackOrClusterStatusInvalid(Stack stack, Cluster cluster) {
        return stack.isStackInDeletionPhase()
                || stack.isStackInStopPhase()
                || stack.isModificationInProgress()
                || cluster == null
                || cluster.isModificationInProgress();
    }

    private void updateClusterStatus(Long stackId, Status stackStatus, Cluster cluster, ClusterStatusResult clusterStatusResult) {
        Status statusInEvent = stackStatus;
        ClusterStatus clusterStatus = clusterStatusResult.getClusterStatus();
        String statusReason = clusterStatusResult.getStatusReason();
        if (isUpdateEnabled(clusterStatus)) {
            if (updateClusterStatus(stackId, cluster, clusterStatus.getClusterStatus())) {
                statusInEvent = clusterStatus.getStackStatus();
            } else {
                statusReason = "The cluster's state is up to date.";
            }
        }
        cloudbreakEventService.fireCloudbreakEvent(stackId, statusInEvent.name(), cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SYNCHRONIZED.code(),
                Collections.singletonList(statusReason)));
    }

    private boolean isUpdateEnabled(ClusterStatus clusterStatus) {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED || clusterStatus == ClusterStatus.AMBIGUOUS;
    }

    private boolean updateClusterStatus(Long stackId, Cluster cluster, Status newClusterStatus) {
        boolean result = false;

        Status status = cluster.getStatus();
        if (status != newClusterStatus) {
            if (!status.equals(Status.MAINTENANCE_MODE_ENABLED) || !newClusterStatus.equals(Status.AVAILABLE)) {
                LOGGER.debug("Cluster {} status is updated from {} to {}", cluster.getId(), status, newClusterStatus);
                clusterService.updateClusterStatusByStackId(stackId, newClusterStatus);
                result = true;
            }
        } else {
            LOGGER.debug("Cluster {} status hasn't changed: {}", cluster.getId(), status);
        }
        return result;
    }

    private enum Msg {
        AMBARI_CLUSTER_COULD_NOT_SYNC("ambari.cluster.could.not.sync"),
        AMBARI_CLUSTER_SYNCHRONIZED("ambari.cluster.synchronized");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
