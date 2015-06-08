package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.EnumSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Component
public class AmbariClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusUpdater.class);

    private EnumSet<Status> failedStatuses = EnumSet.of(Status.CREATE_FAILED, Status.START_FAILED, Status.STOP_FAILED);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AmbariClusterStatusFactory clusterStatusFactory;

    public void updateClusterStatus(Stack stack) {
        if (isClusterStatusCheckNecessary(stack)) {
            Cluster cluster = stack.getCluster();
            String blueprintName = cluster != null ? cluster.getBlueprint().getBlueprintName() : null;
            AmbariClusterStatus clusterStatus = clusterStatusFactory.createClusterStatus(ambariClientProvider.getAmbariClient(
                    cluster.getAmbariIp(), cluster.getUserName(), cluster.getPassword()), blueprintName);
            updateClusterStatus(stack, clusterStatus);
        }
    }

    private boolean isClusterStatusCheckNecessary(Stack stack) {
        boolean result = false;
        Cluster cluster = stack.getCluster();
        if (stack.isDeleteCompleted() && cluster != null) {
            result = failedStatuses.contains(stack.getStatus()) || failedStatuses.contains(cluster.getStatus())
                    || (stack.isAvailable() && (cluster.isAvailable() || cluster.isStopped()));
        }
        return result;
    }

    private boolean updateClusterStatus(Stack stack, AmbariClusterStatus ambariClusterStatus) {
        boolean result = false;
        if (ambariClusterStatus != null && isUpdateEnabled(ambariClusterStatus.getStatus())) {
            boolean clusterUpdated = updateClusterStatus(stack, ambariClusterStatus.getClusterStatus());
            boolean stackUpdated = updateStackStatus(stack, ambariClusterStatus.getStackStatus(), ambariClusterStatus.getStatusReason());
            if (clusterUpdated && !stackUpdated) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), ambariClusterStatus.getStackStatus().name(), ambariClusterStatus.getStatusReason());
            }
            result = stackUpdated || clusterUpdated;
        }
        return result;
    }

    private boolean isUpdateEnabled(ClusterStatus clusterStatus) {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED;
    }

    private boolean updateClusterStatus(Stack stack, Status newClusterStatus) {
        Cluster cluster = stack.getCluster();
        boolean result = false;
        if (cluster.getStatus() != newClusterStatus) {
            LOGGER.info("Cluster {} status is updated from {} to {}", cluster.getId(), cluster.getStatus(), newClusterStatus);
            cluster.setStatus(newClusterStatus);
            clusterRepository.save(cluster);
            result = true;
        }
        return result;
    }

    private boolean updateStackStatus(Stack stack, Status newStackStatus, String statusReason) {
        boolean result = false;
        if (stack.getStatus() != newStackStatus) {
            LOGGER.info("Stack {} status is updated from {} to {}", stack.getId(), stack.getStatus(), newStackStatus);
            stackUpdater.updateStackStatus(stack.getId(), newStackStatus, statusReason);
            result = true;
        }
        return result;
    }
}
