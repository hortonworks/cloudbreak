package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Component
public class AmbariClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusUpdater.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AmbariClusterStatusFactory clusterStatusFactory;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void updateClusterStatus(Stack stack, Cluster cluster) throws CloudbreakSecuritySetupException {
        if (isStackOrClusterStatusInvalid(stack, cluster)) {
            LOGGER.warn("Cluster could not be synchronized while stack is in {} state and cluster is in {} state!",
                    stack.getStatus(), cluster.getStatus());
        } else {
            Long stackId = stack.getId();
            String blueprintName = cluster != null ? cluster.getBlueprint().getBlueprintName() : null;
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
            AmbariClusterStatus clusterStatus = clusterStatusFactory.createClusterStatus(ambariClientProvider.getAmbariClient(
                    clientConfig, cluster.getUserName(), cluster.getPassword()), blueprintName);
            updateClusterStatus(stackId, stack.getStatus(), cluster, clusterStatus);
        }
    }

    private boolean isStackOrClusterStatusInvalid(Stack stack, Cluster cluster) {
        return stack.isStackInDeletionPhase()
                || stack.isStackInStopPhase()
                || stack.isModificationInProgress()
                || cluster.isModificationInProgress();
    }

    private void updateClusterStatus(Long stackId, Status stackStatus, Cluster cluster, AmbariClusterStatus ambariClusterStatus) {
        Status statusInEvent = stackStatus;
        String statusReason;
        if (ambariClusterStatus != null && isUpdateEnabled(ambariClusterStatus.getStatus())) {
            if (updateClusterStatus(stackId, cluster, ambariClusterStatus.getClusterStatus())) {
                statusInEvent = ambariClusterStatus.getStackStatus();
                statusReason = ambariClusterStatus.getStatusReason();
            } else {
                statusReason = "The cluster's state is up to date.";
            }
        } else {
            statusReason = "There are stopped and running Ambari services as well. Restart or stop all of them and try syncing later.";
        }
        cloudbreakEventService.fireCloudbreakEvent(stackId, statusInEvent.name(), "Synced cluster state with Ambari: " + statusReason);
    }

    private boolean isUpdateEnabled(ClusterStatus clusterStatus) {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED;
    }

    private boolean updateClusterStatus(Long stackId, Cluster cluster, Status newClusterStatus) {
        boolean result = false;
        if (cluster.getStatus() != newClusterStatus) {
            LOGGER.info("Cluster {} status is updated from {} to {}", cluster.getId(), cluster.getStatus(), newClusterStatus);
            clusterService.updateClusterStatusByStackId(stackId, newClusterStatus);
            result = true;
        } else {
            LOGGER.info("Cluster {} status hasn't changed: {}", cluster.getId(), cluster.getStatus());
        }
        return result;
    }
}
