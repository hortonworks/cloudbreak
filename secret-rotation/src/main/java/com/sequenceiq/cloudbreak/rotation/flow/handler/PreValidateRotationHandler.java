package com.sequenceiq.cloudbreak.rotation.flow.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.flow.config.SecretRotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.event.PreValidateRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.event.PreValidateRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class PreValidateRotationHandler extends ExceptionCatcherEventHandler<PreValidateRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PreValidateRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PreValidateRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), e,
                SecretRotationException.getFailedStepFromException(e));
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PreValidateRotationTriggerEvent> event) {
        secretRotationService.executePreValidation(event.getData().getSecretType(), event.getData().getResourceCrn(), event.getData().getExecutionType());
        return PreValidateRotationFinishedEvent.fromPayload(event.getData());
    }
}
