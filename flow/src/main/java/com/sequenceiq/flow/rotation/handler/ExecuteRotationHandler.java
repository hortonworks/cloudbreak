package com.sequenceiq.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationTriggerEvent;
import com.sequenceiq.flow.rotation.service.SecretRotationService;

@Component
public class ExecuteRotationHandler extends ExceptionCatcherEventHandler<ExecuteRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecuteRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecuteRotationTriggerEvent> event) {
        return ExecuteRotationFailedEvent.fromPayload(event.getData(), e, SecretRotationException.getFailedStepFromException(e));
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecuteRotationTriggerEvent> event) {
        secretRotationService.executeRotation(event.getData().getSecretType(), event.getData().getResourceCrn(), event.getData().getExecutionType());
        return ExecuteRotationFinishedEvent.fromPayload(event.getData());
    }
}
