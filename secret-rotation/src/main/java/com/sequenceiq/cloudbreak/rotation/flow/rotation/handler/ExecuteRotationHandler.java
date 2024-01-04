package com.sequenceiq.cloudbreak.rotation.flow.rotation.handler;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationOrchestrationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExecuteRotationHandler extends ExceptionCatcherEventHandler<ExecuteRotationTriggerEvent> {

    @Inject
    private SecretRotationOrchestrationService secretRotationOrchestrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecuteRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecuteRotationTriggerEvent> event) {
        return ExecuteRotationFailedEvent.fromPayload(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecuteRotationTriggerEvent> event) {
        ExecuteRotationTriggerEvent rotationEvent = event.getData();
        secretRotationOrchestrationService.rotateIfNeeded(rotationEvent.getSecretType(), rotationEvent.getResourceCrn(),
                rotationEvent.getExecutionType(), rotationEvent.getAdditionalProperties());
        return ExecuteRotationFinishedEvent.fromPayload(rotationEvent);
    }
}
