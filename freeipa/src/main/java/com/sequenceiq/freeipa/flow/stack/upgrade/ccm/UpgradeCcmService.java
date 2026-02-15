package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_IN_PROGRESS;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmContext;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRegisterClusterProxyHandler;
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

    @Value("${cdp.ccm.v2.servers.cidr:}")
    private String ccmV2ServersCidr;

    public void checkPrerequsities(Long stackId, Tunnel oldTunnel) {
        checkFreeIpaIsOnLatestImage(stackId);
        if (oldTunnel == Tunnel.CCM && StringUtils.isNotBlank(ccmV2ServersCidr)) {
            LOGGER.debug("Old tunnel is CCMv1 and CCMv2 servers CIDR is provided: {}, running connectivity test to CCMv2 servers", ccmV2ServersCidr);
            checkCcmV2ServerConnectivity(stackId);
        }
    }

    private void checkFreeIpaIsOnLatestImage(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        ImageEntity stackImage = imageService.getByStackId(stackId);
        FreeIpaUpgradeOptions freeIpaUpgradeOptions = upgradeService.collectUpgradeOptions(stack.getAccountId(), stack.getEnvironmentCrn(),
                Objects.requireNonNullElse(stackImage.getImageCatalogName(), stackImage.getImageCatalogUrl()), false);
        if (!freeIpaUpgradeOptions.getImages().isEmpty()) {
            throw new CloudbreakServiceException("FreeIPA is not on the latest available image. Please upgrade that first. CCM upgrade is not possible yet.");
        }
    }

    private void checkCcmV2ServerConnectivity(Long stackId) {
        Map<String, String> result = upgradeCcmOrchestratorService.checkCcmV2Connectivity(stackId, ccmV2ServersCidr);
        LOGGER.debug("Returned result from CCMv2 Connectivity Check: {}", result);
        if (result != null) {
            String failures = result.values().stream()
                    .peek(resultJson -> LOGGER.debug("Trying to parse JSON: {}", resultJson))
                    .map(resultJson -> JsonUtil.readValueOpt(resultJson, ConnectivityCheckResponse.class))
                    .peek(optResult -> {
                        if (optResult.isEmpty()) {
                            LOGGER.debug("Failed to parse, returned empty value.");
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(response -> response.result() == CheckResult.FAILED)
                    .map(ConnectivityCheckResponse::reason)
                    .distinct()
                    .collect(Collectors.joining("\n"));
            if (StringUtils.isBlank(failures)) {
                LOGGER.info("Connectivity check to {} has passed", ccmV2ServersCidr);
            } else {
                String message = String.format("Connectivity check to %s has failed. Reasons: %s", ccmV2ServersCidr, failures);
                throw new CloudbreakServiceException(message);
            }
        }
    }

    public void checkPrerequisitesState(Long stackId) {
        String statusReason = "Checking prerequisites";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void changeTunnelState(Long stackId) {
        String statusReason = "Changing tunnel type in database";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void changeTunnel(Long stackId, Tunnel tunnel) {
        LOGGER.info("Changing {} stack tunnel type to: {}", stackId, tunnel);
        stackService.setTunnelByStackId(stackId, tunnel);
    }

    public void obtainAgentDataState(Long stackId) {
        String statusReason = "Provisioning connectivity agent";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void obtainAgentData(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        CcmConnectivityParameters ccmConnectivityParameters = ccmUserDataService.fetchAndSaveCcmParameters(stack);
        stack = stackService.getStackById(stackId);
        stack.setCcmParameters(ccmConnectivityParameters);
        stackService.save(stack);
    }

    public void pushSaltStatesState(Long stackId) {
        String statusReason = "Pushing Salt states";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void pushSaltStates(Long stackId) throws CloudbreakOrchestratorException {
        InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
        pushSaltStatesClearPillars(stackId);
        pushAndRefreshPillars(stackId);
    }

    public void upgradeState(Long stackId) {
        String statusReason = "Applying upgrade on nodes";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void upgrade(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.applyUpgradeState(stackId);
    }

    public void reconfigureNginxState(Long stackId) {
        String statusReason = "Reconfiguring NGINX on nodes";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.reconfigureNginx(stackId);
    }

    public void registerClusterProxyState(Long stackId) {
        String statusReason = "Registering into Cluster Proxy";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void registerClusterProxyAndCheckHealth(Long stackId) {
        registerClusterProxy(stackId);
        healthCheck(stackId);
    }

    private void registerClusterProxy(Long stackId) {
        Optional<ConfigRegistrationResponse> configRegistrationResponse = clusterProxyService.registerFreeIpa(stackId);
        configRegistrationResponse.ifPresentOrElse(c -> LOGGER.debug(c.toString()), () -> LOGGER.debug("No clusterproxy register response for {}", stackId));
    }

    private void healthCheck(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        LOGGER.info("Running health check for {}", stack.getName());
        HealthDetailsFreeIpaResponse healthDetails = healthService.getHealthDetails(stack.getEnvironmentCrn(), stack.getAccountId());
        if (healthDetails.getStatus() != Status.AVAILABLE) {
            LOGGER.info("FreeIPA stack {} ({}) is not AVAILABLE. Details: {}", stack.getName(), stack.getResourceCrn(), healthDetails);
            throw new CloudbreakServiceException("One or more FreeIPA instance is not available. Need to roll back CCM upgrade to previous version.");
        }
    }

    public void removeMinaState(Long stackId) {
        String statusReason = "Removing Mina agent from nodes";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void removeMina(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.disableMina(stackId);
    }

    public void deregisterMinaState(Long stackId) {
        String statusReason = "Deregistering Mina agent";
        stackUpdater.updateStackStatus(stackId, UPGRADE_CCM_IN_PROGRESS, statusReason);
    }

    public void deregisterMina(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(userCrn, stack.getAccountId(), keyId,
                stack.getMinaSshdServiceId());
    }

    public void finalizeConfiguration(Long stackId) throws CloudbreakOrchestratorException {
        upgradeCcmOrchestratorService.finalizeConfiguration(stackId);
    }

    public void finishedState(Long stackId, Boolean minaRemoved) {
        InMemoryStateStore.deleteStack(stackId);
        String statusReason = "Upgrade CCM completed";
        if (!minaRemoved) {
            statusReason += ", but removing MINA was unsuccessful.";
        }
        stackUpdater.updateStackStatus(stackId, AVAILABLE, statusReason);
    }

    public void failedState(UpgradeCcmContext context, UpgradeCcmFailureEvent payload) {
        InMemoryStateStore.deleteStack(payload.getResourceId());
        DetailedStackStatus detailedStatus = payload.getTransitionStatusAfterFailure();
        stackService.setTunnelByStackId(payload.getResourceId(), payload.getOldTunnel());
        if (payload.getFailureOrigin().equals(UpgradeCcmRegisterClusterProxyHandler.class)) {
            LOGGER.info("Re-registering cluster proxy to previous tunnel {}", payload.getOldTunnel());
            registerClusterProxy(payload.getResourceId());
        }
        String statusReason = "Upgrade CCM failed: " + payload.getException().getMessage();
        stackUpdater.updateStackStatus(context.getStack(), detailedStatus, statusReason);
    }

    private void pushSaltStatesClearPillars(Long stackId) throws CloudbreakOrchestratorException {
        bootstrapService.bootstrap(stackId);
    }

    private void pushAndRefreshPillars(Long stackId) throws CloudbreakOrchestratorException {
        freeIpaOrchestrationConfigService.configureOrchestrator(stackId);
    }
}