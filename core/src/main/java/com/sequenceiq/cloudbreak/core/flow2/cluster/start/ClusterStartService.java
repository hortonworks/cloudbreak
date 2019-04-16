package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterStartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUtil stackUtil;

    public void startingCluster(StackView stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, String.format("Starting the cluster. Cluster manager ip: %s",
                stackUtil.extractClusterManagerIp(stack)));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.CLUSTER_STARTING, Status.UPDATE_IN_PROGRESS.name(), stackUtil.extractClusterManagerIp(stack));
    }

    public void clusterStartFinished(StackView stack) {
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId())
                .orElseThrow(NotFoundException.notFound("cluster", stack.getId()));
        String ambariIp = stackUtil.extractClusterManagerIp(stack);
        cluster.setUpSince(new Date().getTime());
        clusterService.updateCluster(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Cluster started.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.CLUSTER_STARTED, Status.AVAILABLE.name(), ambariIp);
    }

    public void handleClusterStartFailure(StackView stackView, String errorReason) {
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.START_FAILED);
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Cluster could not be started: " + errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Msg.CLUSTER_START_FAILED, Status.START_FAILED.name(), errorReason);
    }
}
