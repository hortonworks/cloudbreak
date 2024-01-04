package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterCertificateRenewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificateRenewService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    void reissueCertificate(long stackId) {
        String statusReason = "Reissuing the certificate of the cluster.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CERTIFICATE_RENEWAL_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CERTIFICATE_REISSUE);
    }

    void redeployCertificateOnCluster(long stackId) {
        String statusReason = "The redeployment of the reissued certificate to the cluster has been started.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CERTIFICATE_REDEPLOY_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CERTIFICATE_REDEPLOY);
    }

    void certificateRenewalFinished(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Renewal of the cluster's certificate finished.");
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_CERTIFICATE_RENEWAL_FINISHED);
    }

    void certificateRenewalFailed(StackView stack, Exception exception) {
        if (stack.getClusterId() != null) {
            Long stackId = stack.getId();
            String errorMessage = ExceptionMessageFormatterUtil.getErrorMessageFromException(exception);
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CERTIFICATE_RENEWAL_FAILED, errorMessage);
            flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CERTIFICATE_RENEWAL_FAILED, errorMessage);
        } else {
            LOGGER.info("Cluster was null. Flow action was not required.");
        }
    }
}
