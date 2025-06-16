package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustSetupValidationService;

@Component
public class ValidationHandler extends ExceptionCatcherEventHandler<TrustSetupValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationHandler.class);

    @Inject
    private TrustSetupValidationService validationService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<TrustSetupValidationRequest> event) {
        return new TrustSetupValidationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<TrustSetupValidationRequest> event) {
        TrustSetupValidationRequest request = event.getData();
        try {
            ValidationResult validationResult = validationService.validateTrustSetup(request.getResourceId());
            if (validationResult.hasError()) {
                return new TrustSetupValidationFailed(request.getResourceId(), new CloudbreakServiceException(validationResult.getFormattedErrors()));
            } else {
                return new TrustSetupValidationSuccess(request.getResourceId());
            }
        } catch (Exception e) {
            LOGGER.error("Validation failed for trust setup", e);
            return new TrustSetupValidationFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(TrustSetupValidationRequest.class);
    }
}
