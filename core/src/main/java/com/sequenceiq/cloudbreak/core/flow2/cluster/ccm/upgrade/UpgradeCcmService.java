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
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.Tunnel;

@Service
public class UpgradeCcmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackService stackService;

    void tunnelUpdateState(Long stackId) {
        String statusReason = "Updating tunnel type of the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_TUNNEL_UPDATE);
    }

    void pushSaltStatesState(Long stackId) {
        String statusReason = "Pushing Salt states to the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_PUSH_SALT_STATES);
    }

    void reconfigureNginxState(Long stackId) {
        String statusReason = "Reconfiguring NGINX on the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_RECONFIGURE_NGINX);
    }

    void registerClusterProxyState(Long stackId) {
        String statusReason = "Renew Cluster Proxy registration";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_REGISTER_CLUSTER_PROXY);
    }

    void healthCheckState(Long stackId) {
        String statusReason = "Performing connectivity test to the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_HEALTH_CHECK);
    }

    void removeAgentState(Long stackId) {
        String statusReason = "Remove previous version's agent from the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_REMOVE_AGENT);
    }

    void deregisterAgentState(Long stackId) {
        String statusReason = "Deregister previous version's agent";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_DEREGISTER_AGENT);
    }

    void ccmUpgradeFinished(Long stackId) {
        String statusReason = "CCM upgrade finished";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FINISHED);
    }

    void ccmUpgradeFailed(Long stackId) {
        String statusReason = "CCM upgrade failed";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FAILED);
    }

    public void updateTunnel(Long stackId) {
        stackService.setTunnelByStackId(stackId, Tunnel.latestUpgradeTarget());
    }

    public void pushSaltState(Long stackId) {
    }

    public void reconfigureNginx(Long stackId) {
    }

    public void registerClusterProxy(Long stackId) {
    }

    public void healthCheck(Long stackId) {
    }

    public void removeAgent(Long stackId) {
    }

    public void deregisterAgent(Long stackId) {
    }
}
