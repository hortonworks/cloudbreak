package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class CcmUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmUpgradeService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    void prepareCcmUpgradeOnCluster(Long stackId) {
        String statusReason = "Preparing CCM upgrade of the cluster.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_PREPARATION);
    }

    void reRegisterClusterToClusterProxyOnCluster(Long stackId) {
        String statusReason = "Renew Cluster Proxy registration.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_REREGISTER);
    }

    void removeAutoSshOnCluster(Long stackId) {
        String statusReason = "Remove autossh from the cluster.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_REMOVE_AUTOSSH);
    }

    void unregisterHostsOnCluster(Long stackId) {
        String statusReason = "Delete hosts from Cluster Proxy registration.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_UNREGISTER_HOSTS);
    }

    void ccmUpgradeFinished(Long stackId) {
        String statusReason = "CCM upgrade finished.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FINISHED);
    }

    void ccmUpgradeFailed(Long stackId) {
        String statusReason = "CCM upgrade failed.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FAILED);
    }

    void ccmUpgradePreparationFailed(Long stackId) {
        String statusReason = "CCM upgrade preparation failed.";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FAILED);
    }

    public void prepare(Long stackId) {
    }

    public void unregister(Long stackId) {
    }

    public void reregister(Long stackId) {
    }

    public void removeAutoSsh(Long stackId) {
    }
}
