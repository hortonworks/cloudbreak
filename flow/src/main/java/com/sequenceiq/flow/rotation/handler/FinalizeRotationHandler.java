package com.sequenceiq.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.flow.rotation.event.FinalizeRotationSuccessEvent;
import com.sequenceiq.flow.rotation.event.FinalizeRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.flow.rotation.service.SecretRotationService;

@Component
public class FinalizeRotationHandler extends ExceptionCatcherEventHandler<FinalizeRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinalizeRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinalizeRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FinalizeRotationTriggerEvent> event) {
        secretRotationService.finalizeRotation(event.getData().getSecretType(), event.getData().getResourceCrn(), event.getData().getExecutionType());
        return FinalizeRotationSuccessEvent.fromPayload(SecretRotationEvent.ROTATION_FINISHED_EVENT.event(), event.getData());
    }
}
