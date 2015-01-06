package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Component
public class AmbariClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusUpdater.class);

    private EnumSet<Status> failedStatuses = EnumSet.of(Status.CREATE_FAILED, Status.START_FAILED, Status.STOP_FAILED);

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private AmbariClientService clientService;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    private AmbariClusterStatusFactory clusterStatusFactoy;

    public void updateClusterStatus(Stack stack) {
        MDCBuilder.buildMdcContext(stack.getCluster());
        if (isClusterStatusCheckNecessary(stack)) {
            Cluster cluster = stack.getCluster();
            String blueprintName = cluster != null ? cluster.getBlueprint().getBlueprintName() : null;
            AmbariClusterStatus clusterStatus = clusterStatusFactoy.createClusterStatus(clientService.create(stack), blueprintName);
            updateClusterStatus(stack, clusterStatus);
        }
    }

    private boolean isClusterStatusCheckNecessary(Stack stack) {
        boolean result = false;
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            Status stackStatus = stack.getStatus();
            Status clusterStatus = cluster.getStatus();
            result = failedStatuses.contains(stackStatus) || failedStatuses.contains(clusterStatus)
                    || (stackStatus == Status.AVAILABLE && (clusterStatus == Status.AVAILABLE || clusterStatus == Status.STOPPED));
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
        Cluster cluster = clusterRepository.findById(stack.getCluster().getId());
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
