package com.sequenceiq.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.flow.rotation.event.RollbackRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.flow.rotation.service.SecretRotationService;

@Component
public class RollbackRotationHandler extends ExceptionCatcherEventHandler<RollbackRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollbackRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollbackRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollbackRotationTriggerEvent> event) {
        SecretRotationStep failedStep = null;
        if (event.getData().getException() instanceof SecretRotationException) {
            failedStep = ((SecretRotationException) event.getData().getException()).getFailedRotationStep();
        }
        secretRotationService.rollbackRotation(event.getData().getSecretType(), event.getData().getResourceCrn(),
                event.getData().getExecutionType(), failedStep);
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), event.getData().getException());
    }
}
