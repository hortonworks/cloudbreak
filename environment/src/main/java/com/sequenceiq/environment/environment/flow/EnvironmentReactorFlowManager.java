package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class EnvironmentReactorFlowManager {

    private final EventBus eventBus;

    private final EventSender eventSender;

    private final ErrorHandlerAwareReactorEventFactory eventFactory;

    public EnvironmentReactorFlowManager(EventBus eventBus, EventSender eventSender, ErrorHandlerAwareReactorEventFactory eventFactory) {
        this.eventBus = eventBus;
        this.eventSender = eventSender;
        this.eventFactory = eventFactory;
    }

    public void triggerCreationFlow(long envId, String envName, String userCrn) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .build();

        Map<String, Object> flowTriggerUsercrn = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
        eventSender.sendEvent(envCreationEvent, new Event.Headers(flowTriggerUsercrn));
    }

    public void triggerDeleteFlow(Environment environment, String userCrn) {
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .build();

        cancelRunningFlows(environment.getId(), environment.getName());
        Map<String, Object> flowTriggerUsercrn = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
        eventSender.sendEvent(envDeleteEvent, new Event.Headers(flowTriggerUsercrn));
    }

    public void cancelRunningFlows(Long environmentId, String environmentName) {
        BaseNamedFlowEvent cancellationEvent = new BaseNamedFlowEvent(Flow2Handler.FLOW_CANCEL, environmentId, environmentName);
        eventBus.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEventWithErrHandler(cancellationEvent));
    }
}
