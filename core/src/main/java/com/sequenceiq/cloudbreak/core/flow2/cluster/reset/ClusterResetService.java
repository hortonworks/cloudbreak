package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Service
public class ClusterResetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResetService.class);

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EmailSenderService emailSenderService;

    public void resetCluster(long stackId) {
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_RESET, Status.UPDATE_IN_PROGRESS.name());
    }

    public void handleResetClusterFinished(ClusterView clusterView) {
        clusterService.cleanupKerberosCredential(clusterView.getId());
    }

    public void handleResetClusterFailure(StackView stackView, Exception exception) {
        Cluster cluster = clusterService.retrieveClusterByStackId(stackView.getId());
        clusterService.cleanupKerberosCredential(cluster.getId());
        String errorMessage = exception instanceof CloudbreakException && exception.getCause() != null
                ? exception.getCause().getMessage() : exception.getMessage();
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.CREATE_FAILED, errorMessage);
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE);
        flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_CREATE_FAILED, Status.CREATE_FAILED.name(), errorMessage);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningFailureEmail(cluster.getOwner(), stackView.getClusterView().getEmailTo(), cluster.getName());
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.AVAILABLE.name());
        }
    }
}
