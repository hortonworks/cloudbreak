package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.chain.FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_VALIDATION_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.TRUST_SETUP_FINISH_VALIDATION_EVENT;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@Service
public class EnvironmentReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentReactorFlowManager.class);

    private final EventSender eventSender;

    private final FlowCancelService flowCancelService;

    private final StackService stackService;

    private final EntitlementService entitlementService;

    private final NodeValidator nodeValidator;

    public EnvironmentReactorFlowManager(EventSender eventSender,
            FlowCancelService flowCancelService, StackService stackService, EntitlementService entitlementService, NodeValidator nodeValidator) {
        this.eventSender = eventSender;
        this.flowCancelService = flowCancelService;
        this.stackService = stackService;
        this.entitlementService = entitlementService;
        this.nodeValidator = nodeValidator;
    }

    public FlowIdentifier triggerCreationFlow(long envId, String envName, String userCrn, String envCrn) {
        LOGGER.info("Environment creation flow triggered.");
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(START_ENVIRONMENT_INITIALIZATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .build();

        return sendEvent(envCreationEvent, userCrn);
    }

    public FlowIdentifier triggerSetupCrossRealmTrust(long envId, String accountId, String envName, String userCrn, String envCrn,
        EnvironmentSetupCrossRealmTrustRequest request) {
        LOGGER.info("Environment cross realm prepare flow triggered.");
        EnvironmentCrossRealmTrustSetupEvent environmentCrossRealmTrustSetupEvent =
                EnvironmentCrossRealmTrustSetupEvent.builder()
                        .withAccepted(new Promise<>())
                        .withSelector(TRUST_SETUP_VALIDATION_EVENT.selector())
                        .withResourceId(envId)
                        .withResourceName(envName)
                        .withResourceCrn(envCrn)
                        .withRealm(request.getRealm())
                        .withFqdn(request.getFqdn())
                        .withAccountId(accountId)
                        .withRemoteEnvironmentCrn(request.getRemoteEnvironmentCrn())
                        .withIp(request.getIp())
                        .withTrustSecret(request.getTrustSecret())
                        .build();

        return sendEvent(environmentCrossRealmTrustSetupEvent, userCrn);
    }

    public FlowIdentifier triggerSetupFinishCrossRealmTrust(long envId, String envName, String userCrn, String envCrn,
        FinishCrossRealmTrustRequest request) {
        LOGGER.info("Environment cross realm finish flow triggered.");
        EnvironmentCrossRealmTrustSetupFinishEvent environmentCrossRealmTrustSetupFinishEvent =
                EnvironmentCrossRealmTrustSetupFinishEvent.builder()
                        .withAccepted(new Promise<>())
                        .withSelector(TRUST_SETUP_FINISH_VALIDATION_EVENT.selector())
                        .withResourceId(envId)
                        .withResourceName(envName)
                        .withResourceCrn(envCrn)
                        .build();

        return sendEvent(environmentCrossRealmTrustSetupFinishEvent, userCrn);
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    public FlowIdentifier triggerDeleteFlow(EnvironmentView environment, String userCrn, boolean forced) {
        LOGGER.info("Environment simple (FreeIPA) deletion flow triggered for '{}', forced={}.", environment.getName(), forced);
        flowCancelService.cancelRunningFlows(environment.getId());
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .withForceDelete(forced)
                .build();

        return sendEvent(envDeleteEvent, userCrn);
    }

    public FlowIdentifier triggerCascadingDeleteFlow(EnvironmentView environment, String userCrn, boolean forced) {
        LOGGER.info("Environment cascading deletion flow triggered for '{}', forced={}.", environment.getName(), forced);
        flowCancelService.cancelRunningFlows(environment.getId());
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(ENV_DELETE_CLUSTERS_TRIGGER_EVENT)
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .withForceDelete(forced)
                .build();

        return sendEvent(envDeleteEvent, userCrn);
    }

    public FlowIdentifier triggerStopFlow(long envId, String envName, String userCrn) {
        LOGGER.info("Environment stop flow triggered.");
        EnvStopEvent envStopEvent = EnvStopEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(EnvStopStateSelectors.ENV_STOP_DATAHUB_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        return sendEvent(envStopEvent, userCrn);
    }

    public FlowIdentifier triggerStartFlow(long envId, String envName, String userCrn, DataHubStartAction dataHubStartAction) {
        LOGGER.info("Environment start flow triggered.");
        EnvStartEvent envStartEvent = EnvStartEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(EnvStartStateSelectors.ENV_START_FREEIPA_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withDataHubStartAction(dataHubStartAction)
                .build();

        return sendEvent(envStartEvent, userCrn);
    }

    public FlowIdentifier triggerStackConfigUpdatesFlow(EnvironmentView environment, String userCrn) {
        stackService.cancelRunningStackConfigUpdates(environment);

        LOGGER.info("Environment stack configurations update flow triggered.");
        EnvStackConfigUpdatesEvent envStackConfigUpdatesEvent = EnvStackConfigUpdatesEvent.Builder
                .anEnvStackConfigUpdatesEvent()
                .withAccepted(new Promise<>())
                .withSelector(
                        EnvStackConfigUpdatesStateSelectors.ENV_STACK_CONFIG_UPDATES_START_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .withResourceCrn(environment.getResourceCrn())
                .build();

        return sendEvent(envStackConfigUpdatesEvent, userCrn);
    }

    public FlowIdentifier triggerEnvironmentProxyConfigModification(EnvironmentDto environment, ProxyConfig proxyConfig) {
        LOGGER.info("Environment proxy config modification flow triggered.");
        EnvProxyModificationDefaultEvent envProxyModificationEvent =
                EnvProxyModificationDefaultEvent.builder()
                        .withSelector(EnvProxyModificationStateSelectors.MODIFY_PROXY_START_EVENT.selector())
                        .withResourceId(environment.getId())
                        .withResourceName(environment.getName())
                        .withResourceCrn(environment.getResourceCrn())
                        .withProxyConfigCrn(getIfNotNull(proxyConfig, ProxyConfig::getResourceCrn))
                        .withPreviousProxyConfigCrn(getIfNotNull(environment.getProxyConfig(), ProxyConfig::getResourceCrn))
                        .build();
        return sendEvent(envProxyModificationEvent, ThreadBasedUserCrnProvider.getUserCrn());
    }

    public FlowIdentifier triggerLoadBalancerUpdateFlow(EnvironmentDto environmentDto, Long envId, String envName, String envCrn,
            PublicEndpointAccessGateway endpointAccessGateway, Set<String> endpointGatewaySubnetIds, String userCrn) {
        LOGGER.info("Load balancer update flow triggered.");
        if (PublicEndpointAccessGateway.ENABLED.equals(endpointAccessGateway) ||
                entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(environmentDto.getAccountId())) {
            if (CollectionUtils.isNotEmpty(endpointGatewaySubnetIds)) {
                LOGGER.debug("Adding Endpoint Gateway with subnet ids {}", endpointGatewaySubnetIds);
            } else {
                LOGGER.debug("Adding Endpoint Gateway using environment subnets.");
            }
        }
        LoadBalancerUpdateEvent loadBalancerUpdateEvent = LoadBalancerUpdateEvent.Builder.aLoadBalancerUpdateEvent()
                .withAccepted(new Promise<>())
                .withSelector(LoadBalancerUpdateStateSelectors.LOAD_BALANCER_UPDATE_START_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withEnvironmentDto(environmentDto)
                .withEndpointAccessGateway(endpointAccessGateway)
                .withSubnetIds(endpointGatewaySubnetIds)
                .build();

        return sendEvent(loadBalancerUpdateEvent, userCrn);
    }

    public FlowIdentifier triggerCcmUpgradeFlow(EnvironmentDto environment, String userCrn) {
        LOGGER.info("Environment CCM upgrade flow triggered for environment {}", environment.getName());
        UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(environment.getResourceCrn())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .withSelector(UpgradeCcmStateSelectors.UPGRADE_CCM_VALIDATION_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = sendEvent(upgradeCcmEvent, userCrn);
        LOGGER.debug("Environment CCM upgrade flow trigger event sent for environment {}", environment.getName());
        return flowIdentifier;
    }

    public FlowIdentifier triggerVerticalScaleFlow(EnvironmentDto environment, String userCrn, VerticalScaleRequest updateRequest) {
        LOGGER.info("Environment Vertical Scale flow triggered for environment {}", environment.getName());
        EnvironmentVerticalScaleEvent environmentVerticalScaleEvent = EnvironmentVerticalScaleEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(environment.getResourceCrn())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .withFreeIPAVerticalScaleRequest(updateRequest)
                .withSelector(EnvironmentVerticalScaleStateSelectors.VERTICAL_SCALING_FREEIPA_VALIDATION_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = sendEvent(environmentVerticalScaleEvent, userCrn);
        LOGGER.debug("Environment Vertical Scale flow trigger event sent for environment {}", environment.getName());
        return flowIdentifier;
    }

    public FlowIdentifier triggerExternalizedComputeClusterCreationFlow(String userCrn, Environment environment) {
        ExternalizedComputeClusterCreationEvent externalizedComputeClusterCreationEvent = ExternalizedComputeClusterCreationEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(environment.getResourceCrn())
                .withResourceId(environment.getId())
                .withResourceName(environment.getResourceName())
                .withSelector(ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = sendEvent(externalizedComputeClusterCreationEvent, userCrn);
        LOGGER.debug("Externalized compute cluster creation flow trigger event sent for environment {}", environment.getName());
        return flowIdentifier;
    }

    public FlowIdentifier triggerExternalizedComputeReinitializationFlow(String userCrn, Environment environment, boolean force) {
        ExternalizedComputeClusterReInitializationEvent externalizedComputeClusterReInitializationEvent =
                ExternalizedComputeClusterReInitializationEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(environment.getResourceCrn())
                .withResourceId(environment.getId())
                .withResourceName(environment.getResourceName())
                .withForce(force)
                .withSelector(ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = sendEvent(externalizedComputeClusterReInitializationEvent, userCrn);
        LOGGER.debug("Externalized compute cluster reinitialization flow trigger event sent for environment {}", environment.getName());
        return flowIdentifier;
    }

    private FlowIdentifier sendEvent(BaseNamedFlowEvent event, String userCrn) {
        nodeValidator.checkForRecentHeartbeat();
        return eventSender.sendEvent(event, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }
}
