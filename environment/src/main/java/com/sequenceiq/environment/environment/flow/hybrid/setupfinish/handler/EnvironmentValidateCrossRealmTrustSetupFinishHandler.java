package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.TRUST_SETUP_FINISH_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentValidateCrossRealmTrustSetupFinishHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupFinishEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateCrossRealmTrustSetupFinishHandler.class);

    @Override
    public String selector() {
        return SETUP_FINISH_TRUST_VALIDATION_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(event.getData(), e, TRUST_SETUP_FINISH_VALIDATION_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        LOGGER.debug("In EnvironmentValidateSetupFinishCrossRealmTrustHandler.accept");
        try {
            LOGGER.debug("CROSS_REALM_TRUST_SETUP_FINISH_EVENT event sent");
            return EnvironmentCrossRealmTrustSetupFinishEvent
                    .builder()
                    .withSelector(TRUST_SETUP_FINISH_EVENT.selector())
                    .withResourceCrn(event.getData().getResourceCrn())
                    .withResourceId(event.getData().getResourceId())
                    .withResourceName(event.getData().getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("CROSS_REALM_FINISH_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(
                    event.getData(), e, TRUST_SETUP_FINISH_VALIDATION_FAILED);
        }
    }
}
