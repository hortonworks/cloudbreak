package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_IN_PROGRESS;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmContext;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.userdata.CcmUserDataService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.upgrade.UpgradeService;
import com.sequenceiq.freeipa.service.upgrade.ccm.UpgradeCcmOrchestratorService;

@Service
public class UpgradeCcmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private CcmUserDataService ccmUserDataService;

    @Inject
    private FreeIpaOrchestrationConfigService freeIpaOrchestrationConfigService;

    @Inject
    private BootstrapService bootstrapService;

    @Inject
    private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private FreeIpaStackHealthDetailsService healthService;

    @Inject
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Inject
    private UpgradeService upgradeService;

    public void checkPrerequsities(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        ImageEntity stackImage = imageService.getByStackId(stackId);
        FreeIpaUpgradeOptions freeIpaUpgradeOptions = upgradeService.collectUpgradeOptions(stack.getAccountId(), stack.getEnvironmentCrn(),
                Objects.requireNonNullElse(stackImage.getImageCatalogName(), stackImage.getImageCatalogUrl()));
        if (!freeIpaUpgradeOptions.getImages().isEmpty()) {
            throw new CloudbreakServiceException("FreeIPA is not on the latest available image. Please upgrade that first. CCM upgrade is not possible yet.");
        }
    }

    public void checkPrerequisitesState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Checking prerequisites";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void changeTunnelState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Changing tunnel type in database";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void changeTunnel(Long stackId) {
        stackService.setTunnelByStackId(stackId, Tunnel.latestUpgradeTarget());
    }

    public void obtainAgentDataState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Provisioning connectivity agent";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void obtainAgentData(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        CcmConnectivityParameters ccmConnectivityParameters = ccmUserDataService.fetchAndSaveCcmParameters(stack);
        stack = stackService.getStackById(stackId);
        stack.setCcmParameters(ccmConnectivityParameters);
        stackService.save(stack);
    }

    public void pushSaltStatesState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Pushing Salt states";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void pushSaltStates(Long stackId) throws CloudbreakOrchestratorException {
        InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
        pushSaltStatesClearPillars(stackId);
        pushAndRefreshPillars(stackId);
    }

    public void upgradeState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Applying upgrade on nodes";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void upgrade(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.applyUpgradeState(stackId);
    }

    public void reconfigureNginxState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Reconfiguring NGINX on nodes";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.reconfigureNginx(stackId);
    }

    public void registerClusterProxyState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Registering into Cluster Proxy";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void registerClusterProxy(Long stackId) {
        Optional<ConfigRegistrationResponse> configRegistrationResponse = clusterProxyService.registerFreeIpa(stackId);
        configRegistrationResponse.ifPresentOrElse(c -> LOGGER.debug(c.toString()), () -> LOGGER.debug("No clusterproxy register response for {}", stackId));
    }

    public void healthCheckState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Running health check";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void healthCheck(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        HealthDetailsFreeIpaResponse healthDetails = healthService.getHealthDetails(stack.getEnvironmentCrn(), stack.getAccountId());
        if (healthDetails.getNodeHealthDetails().stream().anyMatch(hd -> !hd.getStatus().isAvailable())) {
            throw new CloudbreakServiceException("One or more FreeIPA instance is not available. Need to roll back CCM upgrade to previous version.");
        }
        // TODO: rollback if bad, but how?
    }

    public void removeMinaState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Removing Mina agent from nodes";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void removeMina(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.disableMina(stackId);
    }

    public void deregisterMinaState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Deregistering Mina agent";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void deregisterMina(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(userCrn, stack.getAccountId(), keyId,
                stack.getMinaSshdServiceId());
    }

    public void finishedState(Long stackId) {
        InMemoryStateStore.deleteStack(stackId);
        DetailedStackStatus detailedStatus = AVAILABLE;
        String statusReason = "Upgrade CCM completed";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void failedState(UpgradeCcmContext context, UpgradeCcmFailureEvent payload) {
        InMemoryStateStore.deleteStack(payload.getResourceId());
        DetailedStackStatus detailedStatus = payload.getTransitionStatusAfterFailure().orElse(UPGRADE_CCM_FAILED);
        String statusReason = "Upgrade CCM failed";
        stackUpdater.updateStackStatus(payload.getResourceId(), detailedStatus, statusReason);
    }

    private void pushSaltStatesClearPillars(Long stackId) throws CloudbreakOrchestratorException {
        bootstrapService.bootstrap(stackId);
    }

    private void pushAndRefreshPillars(Long stackId) throws CloudbreakOrchestratorException {
        freeIpaOrchestrationConfigService.configureOrchestrator(stackId);
    }
}
