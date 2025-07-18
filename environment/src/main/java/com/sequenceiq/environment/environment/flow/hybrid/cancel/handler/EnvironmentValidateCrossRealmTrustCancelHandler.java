package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentValidateCrossRealmTrustCancelHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustCancelEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateCrossRealmTrustCancelHandler.class);

    private final EnvironmentService environmentService;

    protected EnvironmentValidateCrossRealmTrustCancelHandler(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return TRUST_CANCEL_VALIDATION_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustCancelEvent> event) {
        return new EnvironmentCrossRealmTrustCancelFailedEvent(event.getData(), e, TRUST_CANCEL_VALIDATION_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> environmentCrossRealmTrustCancelEvent) {
        EnvironmentCrossRealmTrustCancelEvent data = environmentCrossRealmTrustCancelEvent.getData();
        LOGGER.debug("In EnvironmentValidateCrossRealmTrustCancelHandler.accept");
        try {
            LOGGER.debug("TRUST_CANCEL_EVENT event sent");
            environmentService.validateCancelCrossRealmSetup();
            return EnvironmentCrossRealmTrustCancelEvent.builder()
                    .withSelector(TRUST_CANCEL_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_CANCEL_FAILED event sent");
            return new EnvironmentCrossRealmTrustCancelFailedEvent(data, e, TRUST_CANCEL_FAILED);
        }
    }
}
