package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.chain.FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
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

    public EnvironmentReactorFlowManager(EventSender eventSender, FlowCancelService flowCancelService) {
        this.eventSender = eventSender;
        this.flowCancelService = flowCancelService;
    }

    public void triggerCreationFlow(long envId, String envName, String userCrn, String envCrn) {
        LOGGER.info("Environment creation flow triggered.");
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(START_ENVIRONMENT_INITIALIZATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .build();

        eventSender.sendEvent(envCreationEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    public void triggerDeleteFlow(Environment environment, String userCrn) {
        LOGGER.info("Environment deletion flow triggered for '{}'.", environment.getName());
        flowCancelService.cancelRunningFlows(environment.getId());
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .build();

        eventSender.sendEvent(envDeleteEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public void triggerForcedDeleteFlow(Environment environment, String userCrn) {
        LOGGER.info("Environment forced deletion flow triggered for '{}'.", environment.getName());
        flowCancelService.cancelRunningFlows(environment.getId());
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(ENV_DELETE_CLUSTERS_TRIGGER_EVENT)
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .build();

        eventSender.sendEvent(envDeleteEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public void triggerStopFlow(long envId, String envName, String userCrn) {
        LOGGER.info("Environment stop flow triggered.");
        EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                .withAccepted(new Promise<>())
                .withSelector(EnvStopStateSelectors.ENV_STOP_DATAHUB_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        eventSender.sendEvent(envStopEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }

    public void triggerStartFlow(long envId, String envName, String userCrn) {
        LOGGER.info("Environment start flow triggered.");
        EnvStartEvent envSrartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                .withAccepted(new Promise<>())
                .withSelector(EnvStartStateSelectors.ENV_START_FREEIPA_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        eventSender.sendEvent(envSrartEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
    }
}
