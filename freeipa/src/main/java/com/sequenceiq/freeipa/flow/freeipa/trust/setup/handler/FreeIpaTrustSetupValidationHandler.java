package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustSetupValidationService;

@Component
public class FreeIpaTrustSetupValidationHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupValidationHandler.class);

    @Inject
    private TrustSetupValidationService validationService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupValidationRequest> event) {
        return new FreeIpaTrustSetupValidationFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupValidationRequest> event) {
        FreeIpaTrustSetupValidationRequest request = event.getData();
        try {
            TaskResults taskResults = validationService.validateTrustSetup(request.getResourceId());
            if (taskResults.hasErrors()) {
                return new FreeIpaTrustSetupValidationFailed(request.getResourceId(), taskResults, VALIDATION);
            } else {
                return new FreeIpaTrustSetupValidationSuccess(request.getResourceId(), taskResults);
            }
        } catch (Exception e) {
            LOGGER.error("Validation failed for trust setup", e);
            return new FreeIpaTrustSetupValidationFailed(request.getResourceId(), e, VALIDATION);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupValidationRequest.class);
    }
}
