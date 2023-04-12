package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.chain.FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

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
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
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
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@Service
public class EnvironmentReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentReactorFlowManager.class);

    private final EventSender eventSender;

    private final FlowCancelService flowCancelService;

    private final StackService stackService;

    private final EntitlementService entitlementService;

    public EnvironmentReactorFlowManager(EventSender eventSender,
            FlowCancelService flowCancelService, StackService stackService, EntitlementService entitlementService) {
        this.eventSender = eventSender;
        this.flowCancelService = flowCancelService;
        this.stackService = stackService;
        this.entitlementService = entitlementService;
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

        return eventSender.sendEvent(envCreationEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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

        return eventSender.sendEvent(envDeleteEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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

        return eventSender.sendEvent(envDeleteEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public FlowIdentifier triggerStopFlow(long envId, String envName, String userCrn) {
        LOGGER.info("Environment stop flow triggered.");
        EnvStopEvent envStopEvent = EnvStopEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(EnvStopStateSelectors.ENV_STOP_DATAHUB_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        return eventSender.sendEvent(envStopEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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

        return eventSender.sendEvent(envStartEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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

        return eventSender.sendEvent(envStackConfigUpdatesEvent,
                new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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
        return eventSender.sendEvent(envProxyModificationEvent, new Event.Headers(getFlowTriggerUsercrn(ThreadBasedUserCrnProvider.getUserCrn())));
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

        return eventSender.sendEvent(loadBalancerUpdateEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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
        FlowIdentifier flowIdentifier = eventSender.sendEvent(upgradeCcmEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
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
        FlowIdentifier flowIdentifier = eventSender.sendEvent(environmentVerticalScaleEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
        LOGGER.debug("Environment Vertical Scale flow trigger event sent for environment {}", environment.getName());
        return flowIdentifier;
    }
}
