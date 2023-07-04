package com.sequenceiq.cloudbreak.rotation.flow.rotation.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RollbackRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollbackRotationHandler extends ExceptionCatcherEventHandler<RollbackRotationTriggerEvent> {

    @Inject
    private SecretRotationService secretRotationService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollbackRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollbackRotationTriggerEvent> event) {
        return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), event.getData(), e, event.getData().getFailedStep());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollbackRotationTriggerEvent> event) {
        RollbackRotationTriggerEvent rollbackEvent = event.getData();
        String resourceCrn = rollbackEvent.getResourceCrn();
        RotationFlowExecutionType executionType = rollbackEvent.getExecutionType();
        SecretType secretType = rollbackEvent.getSecretType();
        Exception exception = rollbackEvent.getException();
        SecretRotationStep failedStep = rollbackEvent.getFailedStep();
        secretRotationUsageService.rollbackStarted(secretType, resourceCrn, executionType);
        try {
            secretRotationService.rollbackRotation(secretType, resourceCrn, executionType, failedStep);
            secretRotationUsageService.rollbackFinished(secretType, resourceCrn, executionType);
            return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), rollbackEvent, exception, failedStep);
        } catch (Exception e) {
            secretRotationUsageService.rollbackFailed(secretType, resourceCrn, e.getMessage(), executionType);
            throw e;
        }
    }
}
