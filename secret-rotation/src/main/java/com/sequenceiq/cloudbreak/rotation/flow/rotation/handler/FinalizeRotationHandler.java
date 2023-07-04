package com.sequenceiq.cloudbreak.rotation.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.FinalizeRotationSuccessEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.FinalizeRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class FinalizeRotationHandler extends ExceptionCatcherEventHandler<FinalizeRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinalizeRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinalizeRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), e,
                SecretRotationException.getFailedStepFromException(e));
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FinalizeRotationTriggerEvent> event) {
        FinalizeRotationTriggerEvent finalizeEvent = event.getData();
        secretRotationService.finalizeRotation(finalizeEvent.getSecretType(), finalizeEvent.getResourceCrn(), finalizeEvent.getExecutionType());
        secretRotationUsageService.rotationFinished(finalizeEvent.getSecretType(), finalizeEvent.getResourceCrn(), finalizeEvent.getExecutionType());
        return FinalizeRotationSuccessEvent.fromPayload(SecretRotationEvent.ROTATION_FINISHED_EVENT.event(), finalizeEvent);
    }
}
