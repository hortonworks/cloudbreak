package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationSuccess;
import com.sequenceiq.freeipa.service.freeipa.crossrealm.CrossRealmValidationService;

@Component
public class ValidationHandler extends ExceptionCatcherEventHandler<CrossRealmTrustValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationHandler.class);

    @Inject
    private CrossRealmValidationService crossRealmValidationService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CrossRealmTrustValidationRequest> event) {
        return new CrossRealmTrustValidationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CrossRealmTrustValidationRequest> event) {
        CrossRealmTrustValidationRequest request = event.getData();
        try {
            LOGGER.info("Validate for IPA Server image trust packages");
            crossRealmValidationService.validateCrossRealmPreparation(request.getResourceId());
            return new CrossRealmTrustValidationSuccess(request.getResourceId());
        } catch (Exception e) {
            return new CrossRealmTrustValidationFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CrossRealmTrustValidationRequest.class);
    }
}
