package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.AbstractUpgradeCcmEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.HealthCheckService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.UpgradeCcmOrchestratorService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Service
public class UpgradeCcmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

    @Inject
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Inject
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private HealthCheckService healthCheckService;

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

    public void ccmUpgradeFinished(Long stackId, Long clusterId) {
        String statusReason = "CCM upgrade finished";
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FINISHED);
    }

    public void ccmUpgradeFailed(Long stackId, Long clusterId, Tunnel oldTunnel,
            Class<? extends ExceptionCatcherEventHandler<? extends AbstractUpgradeCcmEvent>> failureOrigin) {

        String statusReason = "CCM upgrade failed";
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        stackService.setTunnelByStackId(stackId, oldTunnel);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CCM_UPGRADE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_CCM_UPGRADE_FAILED);
    }

    public void updateTunnel(Long stackId) {
        stackService.setTunnelByStackId(stackId, Tunnel.latestUpgradeTarget());
    }

    public void pushSaltState(Long stackId, Long clusterId) {
        InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
        if (clusterId != null) {
            InMemoryStateStore.putCluster(clusterId, PollGroup.POLLABLE);
        }
        clusterServiceRunner.redeployStates(stackId);
        clusterServiceRunner.redeployGatewayPillar(stackId);
    }

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.reconfigureNginx(stackId);
    }

    public void registerClusterProxy(Long stackId) {
        Optional<ConfigRegistrationResponse> configRegistrationResponse = clusterProxyService.reRegisterCluster(stackId);
        configRegistrationResponse.ifPresentOrElse(c -> LOGGER.debug(c.toString()), () -> LOGGER.debug("No clusterproxy register response for {}", stackId));
    }

    public void healthCheck(Long stackId) {
        Set<String> unhealthyHosts;
        try {
            unhealthyHosts = healthCheckService.getUnhealthyHosts(stackId);
        } catch (RuntimeException ex) {
            throw new CloudbreakServiceException("Cannot get host statuses, CM is likely not accessible. Need to roll back CCM upgrade to previous version.");
        }
        if (!unhealthyHosts.isEmpty()) {
            LOGGER.info("There are unhealthy hosts after registering to Cluster Proxy: {}", unhealthyHosts);
            throw new CloudbreakServiceException("One or more Gateway instance is not available. Need to roll back CCM upgrade to previous version.");
        } else {
            LOGGER.info("All hosts are healthy after registering to Cluster Proxy.");
        }
    }

    public void removeAgent(Long stackId, Tunnel oldTunnel) throws CloudbreakOrchestratorException {
        if (oldTunnel == Tunnel.CCM) {
            upgradeCcmOrchestratorService.disableMina(stackId);
        } else if (oldTunnel == Tunnel.CCMV2) {
            upgradeCcmOrchestratorService.disableInvertingProxyAgent(stackId);
        }
    }

    public void deregisterAgent(Long stackId, Tunnel oldTunnel) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (oldTunnel == Tunnel.CCM) {
            String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            LOGGER.debug("Deregistering agent from {} with key ID {} and user CRN {}", oldTunnel, keyId, userCrn);
            ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(
                    userCrn, Crn.safeFromString(stack.getResourceCrn()).getAccountId(), keyId, stack.getMinaSshdServiceId());
        } else if (oldTunnel == Tunnel.CCMV2) {
            LOGGER.debug("Deregistering agent from {} with agent CRN {}", oldTunnel, stack.getCcmV2AgentCrn());
            ccmV2AgentTerminationListener.deregisterInvertingProxyAgent(stack.getCcmV2AgentCrn());
        }
    }
}
