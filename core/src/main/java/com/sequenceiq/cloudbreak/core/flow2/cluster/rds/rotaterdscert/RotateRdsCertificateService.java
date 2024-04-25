package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class RotateRdsCertificateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotateRdsCertificateService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    void checkPrerequisitesState(Long stackId) {
        String statusReason = "Checking cluster prerequisites for RDS certificate rotation";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES);
    }

    void getLatestRdsCertificateState(Long stackId) {
        String statusReason = "Obtaining the latest RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_GET_LATEST);
    }

    void updateLatestRdsCertificateState(Long stackId) {
        String statusReason = "Pushing latest RDS certificate to the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_PUSH_LATEST);
    }

    void restartCmState(Long stackId) {
        String statusReason = "Restarting Cluster Manager service";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CM_RESTART);
    }

    void rollingRestartRdsCertificateState(Long stackId) {
        String statusReason = "Restarting cluster services";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ROLLING_SERVICE_RESTART);
    }

    void rotateOnProviderState(Long stackId) {
        String statusReason = "Rotating RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER);
    }

    void rotateRdsCertFinished(Long stackId) {
        String statusReason = "RDS certificate rotation finished";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED);
    }

    void rotateRdsCertFailed(RotateRdsCertificateFailedEvent failedEvent) {
        String statusReason = "RDS certificate rotation failed: " + failedEvent.getException().getMessage();
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(failedEvent.getResourceId(), DetailedStackStatus.ROTATE_RDS_CERTIFICATE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(failedEvent.getResourceId(), UPDATE_FAILED.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED);
    }

    public void checkPrerequisites(Long stackId) {

    }

    public void getLatestRdsCertificate(Long stackId) {

    }

    public void updateLatestRdsCertificate(Long stackId) {

    }

    public void restartCm(Long stackId) {

    }

    public void rollingRestartServices(Long stackId) {

    }

    public void rotateOnProvider(Long stackId) {

    }
}
