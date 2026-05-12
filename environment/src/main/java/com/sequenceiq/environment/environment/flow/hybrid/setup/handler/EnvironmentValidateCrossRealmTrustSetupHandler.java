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
import com.sequenceiq.environment.environment.service.ClusterAvailabilityValidator;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentValidateCrossRealmTrustSetupHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateCrossRealmTrustSetupHandler.class);

    private final ClusterAvailabilityValidator clusterAvailabilityValidator;

    public EnvironmentValidateCrossRealmTrustSetupHandler(ClusterAvailabilityValidator clusterAvailabilityValidator) {
        this.clusterAvailabilityValidator = clusterAvailabilityValidator;
    }

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
            EnvironmentCrossRealmTrustSetupEvent data = environmentCrossRealmTrustSetupEvent.getData();
            LOGGER.debug("Validating cluster availability for environment CRN: {}", data.getResourceCrn());
            clusterAvailabilityValidator.validateAllClustersAvailable(data.getResourceCrn(), "Cross-realm trust setup");
            LOGGER.debug("TRUST_SETUP_EVENT event sent");
            return data.toBuilder()
                    .withSelector(TRUST_SETUP_EVENT.selector())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_SETUP_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFailedEvent(
                    environmentCrossRealmTrustSetupEvent.getData(), e, TRUST_SETUP_VALIDATION_FAILED);
        }
    }
}
