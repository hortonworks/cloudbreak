package com.sequenceiq.cloudbreak.rotation.flow.rotation.handler;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.PreValidateRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.PreValidateRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationOrchestrationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class PreValidateRotationHandler extends ExceptionCatcherEventHandler<PreValidateRotationTriggerEvent> {

    @Inject
    private SecretRotationOrchestrationService secretRotationOrchestrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PreValidateRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PreValidateRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PreValidateRotationTriggerEvent> event) {
        secretRotationOrchestrationService.preValidateIfNeeded(event.getData().getSecretType(), event.getData().getResourceCrn(),
                event.getData().getExecutionType(), event.getData().getAdditionalProperties());
        return PreValidateRotationFinishedEvent.fromPayload(event.getData());
    }
}
