package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class ClusterCertificateRenewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    void reissueCertificate(long stackId) {
        String statusReason = "Reissuing the certificate of the cluster.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, statusReason);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CERTIFICATE_REISSUE);
    }

    void redeployCertificateOnCluster(long stackId) {
        String statusReason = "The redeployment of the reissued certificate to the cluster has been started.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, statusReason);
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CERTIFICATE_REDEPLOY);
    }

    void certificateRenewalFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Renewal of the cluster's certificate finished.");
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_CERTIFICATE_RENEWAL_FINISHED);
    }

    void certificateRenewalFailed(StackView stackView, Exception exception) {
        if (stackView.getClusterView() != null) {
            Long stackId = stackView.getId();
            String errorMessage = ExceptionMessageFormatterUtil.getErrorMessageFromException(exception);
            clusterService.updateClusterStatusByStackId(stackId, UPDATE_FAILED, errorMessage);
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
            flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CERTIFICATE_RENEWAL_FAILED, errorMessage);
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }
}
