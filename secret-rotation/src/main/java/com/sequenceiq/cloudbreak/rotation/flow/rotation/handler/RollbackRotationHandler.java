package com.sequenceiq.cloudbreak.rotation.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRollbackFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RollbackRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationOrchestrationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollbackRotationHandler extends ExceptionCatcherEventHandler<RollbackRotationTriggerEvent> {

    @Inject
    private SecretRotationOrchestrationService secretRotationOrchestrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollbackRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollbackRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollbackRotationTriggerEvent> event) {
        RollbackRotationTriggerEvent rollbackEvent = event.getData();
        secretRotationOrchestrationService.rollbackIfNeeded(rollbackEvent.getSecretType(), rollbackEvent.getResourceCrn(), rollbackEvent.getExecutionType(),
                rollbackEvent.getRollbackReason());
        return ExecuteRollbackFinishedEvent.fromPayload(rollbackEvent);
    }
}
