package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOPPING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_FAILED;

import java.time.Duration;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterStopService {
    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUtil stackUtil;

    public void stoppingCluster(long stackId) {
        updateClusterUptime(stackId);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_STOPPING);
        clusterService.updateClusterStatusByStackId(stackId, Status.STOP_IN_PROGRESS);
    }

    private void updateClusterUptime(long stackId) {
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stackId)
                .orElseThrow(NotFoundException.notFound("cluster", stackId));
        cluster.setUptime(Duration.ofMillis(stackUtil.getUptimeForCluster(cluster, true)).toString());
        clusterService.updateCluster(cluster);
    }

    public void clusterStopFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, Status.STOPPED);
        flowMessageService.fireEventAndLog(stackId, Status.STOPPED.name(), CLUSTER_STOPPED);
    }

    public void handleClusterStopFailure(StackView stackView, String errorReason) {
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.STOPPED, errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Status.STOP_FAILED.name(), CLUSTER_STOP_FAILED, errorReason);
    }
}
