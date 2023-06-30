package com.sequenceiq.flow.rotation.handler;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.usage.SecretRotationUsageProcessor;
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

    @Inject
    private Optional<SecretRotationUsageProcessor> secretRotationUsageProcessor;

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
        secretRotationUsageProcessor.ifPresent(processor -> processor.rollbackStarted(secretType, resourceCrn, executionType));
        try {
            secretRotationService.rollbackRotation(secretType, resourceCrn, executionType, failedStep);
            secretRotationUsageProcessor.ifPresent(processor -> processor.rollbackFinished(secretType, resourceCrn, executionType));
            return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), rollbackEvent, exception, failedStep);
        } catch (Exception e) {
            secretRotationUsageProcessor.ifPresent(processor -> processor.rollbackFailed(secretType, resourceCrn, e.getMessage(), executionType));
            throw e;
        }
    }
}
