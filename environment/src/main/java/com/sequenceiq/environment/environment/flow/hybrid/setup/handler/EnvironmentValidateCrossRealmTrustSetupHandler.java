package com.sequenceiq.environment.environment.flow.hybrid.setup.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentValidateCrossRealmTrustSetupHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateCrossRealmTrustSetupHandler.class);

    @Override
    public String selector() {
        return TRUST_SETUP_VALIDATION_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFailedEvent(event.getData(), e, TRUST_SETUP_VALIDATION_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupEvent> environmentCrossRealmTrustSetupEvent) {
        LOGGER.debug("In EnvironmentValidateCrossRealmTrustSetupHandler.accept");
        try {
            LOGGER.debug("TRUST_SETUP_EVENT event sent");
            EnvironmentCrossRealmTrustSetupEvent data = environmentCrossRealmTrustSetupEvent.getData();
            return EnvironmentCrossRealmTrustSetupEvent.builder()
                    .withSelector(TRUST_SETUP_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .withAccountId(data.getAccountId())
                    .withFqdn(data.getFqdn())
                    .withRealm(data.getRealm())
                    .withRemoteEnvironmentCrn(data.getRemoteEnvironmentCrn())
                    .withIp(data.getIp())
                    .withTrustSecret(data.getTrustSecret())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_SETUP_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFailedEvent(
                    environmentCrossRealmTrustSetupEvent.getData(), e, TRUST_SETUP_VALIDATION_FAILED);
        }
    }
}
