package com.sequenceiq.cloudbreak.rotation.flow.subrotation.handler;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SubRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationOrchestrationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExecuteSubRotationHandler extends ExceptionCatcherEventHandler<ExecuteSubRotationTriggerEvent> {

    @Inject
    private SecretRotationOrchestrationService secretRotationOrchestrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecuteSubRotationTriggerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecuteSubRotationTriggerEvent> event) {
        return SubRotationFailedEvent.fromPayload(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecuteSubRotationTriggerEvent> event) {
        ExecuteSubRotationTriggerEvent subRotationEvent = event.getData();
        SecretType secretType = subRotationEvent.getSecretType();
        String resourceCrn = subRotationEvent.getResourceCrn();
        RotationFlowExecutionType executionType = subRotationEvent.getExecutionType();
        Map<String, String> additionalProperties = subRotationEvent.getAdditionalProperties();
        switch (executionType) {
            case PREVALIDATE -> secretRotationOrchestrationService.preValidateIfNeeded(secretType, resourceCrn, executionType, additionalProperties);
            case ROTATE -> secretRotationOrchestrationService.rotateIfNeeded(secretType, resourceCrn, executionType, additionalProperties);
            case ROLLBACK -> secretRotationOrchestrationService.rollbackIfNeeded(secretType, resourceCrn, executionType, additionalProperties,
                    new SecretRotationException("Explicit rollback"));
            case FINALIZE -> secretRotationOrchestrationService.finalizeIfNeeded(secretType, resourceCrn, executionType, additionalProperties);
            default -> throw new UnsupportedOperationException("Invalid or missing execution type: " + executionType);
        }
        return ExecuteSubRotationFinishedEvent.fromPayload(subRotationEvent);
    }
}
