package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_TRUST_ENTITY_DELETE_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_TRUST_ENTITY_DELETE_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_SALT_UPDATE_EVENT;

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

/**
 * Deletes the FreeIPA trust entity after CM configuration has already been cleaned up.
 * This step is intentionally placed last so that the realm name remains available
 * in FreeIPA for the preceding config-removal step, even if that step needs to be retried.
 */
@Component
public class EnvironmentCrossRealmTrustEntityDeleteHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustCancelEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustEntityDeleteHandler.class);

    private final EnvironmentService environmentService;

    protected EnvironmentCrossRealmTrustEntityDeleteHandler(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return TRUST_CANCEL_TRUST_ENTITY_DELETE_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustCancelEvent> event) {
        return new EnvironmentCrossRealmTrustCancelFailedEvent(event.getData(), e, TRUST_CANCEL_TRUST_ENTITY_DELETE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustEntityDeleteHandler.accept");
        EnvironmentCrossRealmTrustCancelEvent data = event.getData();
        try {
            LOGGER.info("Removing remote environment CRN (trust entity) for environment: {}", data.getResourceCrn());
            environmentService.removeRemoteEnvironmentCrn(data.getResourceCrn());
            LOGGER.debug("TRUST_CANCEL_SALT_UPDATE_EVENT event sent");
            return EnvironmentCrossRealmTrustCancelEvent.builder()
                    .withSelector(TRUST_CANCEL_SALT_UPDATE_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_CANCEL_TRUST_ENTITY_DELETE_FAILED event sent");
            return new EnvironmentCrossRealmTrustCancelFailedEvent(data, e, TRUST_CANCEL_TRUST_ENTITY_DELETE_FAILED);
        }
    }
}

