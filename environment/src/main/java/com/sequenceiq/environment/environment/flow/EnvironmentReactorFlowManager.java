package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.chain.FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent.EnvStackConfigUpdatesEventBuilder;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class EnvironmentReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentReactorFlowManager.class);

    private final EventSender eventSender;

    private final FlowCancelService flowCancelService;

    private final StackService stackService;

    public EnvironmentReactorFlowManager(EventSender eventSender,
            FlowCancelService flowCancelService, StackService stackService) {
        this.eventSender = eventSender;
        this.flowCancelService = flowCancelService;
        this.stackService = stackService;
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
        LOGGER.info("Environment deletion flow triggered for '{}'.", environment.getName());
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
        LOGGER.info("Environment forced deletion flow triggered for '{}'.", environment.getName());
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
        EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                .withAccepted(new Promise<>())
                .withSelector(EnvStopStateSelectors.ENV_STOP_DATAHUB_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        return eventSender.sendEvent(envStopEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public FlowIdentifier triggerStartFlow(long envId, String envName, String userCrn, DataHubStartAction dataHubStartAction) {
        LOGGER.info("Environment start flow triggered.");
        EnvStartEvent envSrartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                .withAccepted(new Promise<>())
                .withSelector(EnvStartStateSelectors.ENV_START_FREEIPA_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withDataHubStart(dataHubStartAction)
                .build();

        return eventSender.sendEvent(envSrartEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public FlowIdentifier triggerStackConfigUpdatesFlow(EnvironmentView environment, String userCrn) {
        stackService.cancelRunningStackConfigUpdates(environment);

        LOGGER.info("Environment stack configurations update flow triggered.");
        EnvStackConfigUpdatesEvent envStackConfigUpdatesEvent = EnvStackConfigUpdatesEventBuilder
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

    public FlowIdentifier triggerLoadBalancerUpdateFlow(EnvironmentDto environmentDto, Long envId, String envName, String envCrn,
            PublicEndpointAccessGateway endpointAccessGateway, Set<String> subnetIds, String userCrn) {
        LOGGER.info("Load balancer update flow triggered.");
        if (PublicEndpointAccessGateway.ENABLED.equals(endpointAccessGateway)) {
            if (subnetIds != null && !subnetIds.isEmpty()) {
                LOGGER.debug("Adding Endpoint Gateway with subnet ids {}", subnetIds);
            } else {
                LOGGER.debug("Adding Endpoint Gateway using environment subnets.");
            }
        }
        LoadBalancerUpdateEvent loadBalancerUpdateEvent = LoadBalancerUpdateEvent.LoadBalancerUpdateEventBuilder.aLoadBalancerUpdateEvent()
                .withAccepted(new Promise<>())
                .withSelector(LoadBalancerUpdateStateSelectors.LOAD_BALANCER_UPDATE_START_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withEnvironmentDto(environmentDto)
                .withPublicEndpointAccessGateway(endpointAccessGateway)
                .withSubnetIds(subnetIds)
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
}
