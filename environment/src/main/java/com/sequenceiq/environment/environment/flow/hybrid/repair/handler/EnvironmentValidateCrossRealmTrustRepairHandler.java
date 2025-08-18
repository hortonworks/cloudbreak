package com.sequenceiq.environment.environment.flow.hybrid.repair.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairHandlerSelectors.TRUST_REPAIR_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.TRUST_REPAIR_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairEvent;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentValidateCrossRealmTrustRepairHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustRepairEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateCrossRealmTrustRepairHandler.class);

    private final EnvironmentService environmentService;

    protected EnvironmentValidateCrossRealmTrustRepairHandler(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return TRUST_REPAIR_VALIDATION_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustRepairEvent> event) {
        return new EnvironmentCrossRealmTrustRepairFailedEvent(event.getData(), e, TRUST_REPAIR_VALIDATION_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustRepairEvent> environmentCrossRealmTrustRepairEvent) {
        EnvironmentCrossRealmTrustRepairEvent data = environmentCrossRealmTrustRepairEvent.getData();
        LOGGER.debug("In EnvironmentValidateCrossRealmTrustRepairHandler.accept");
        try {
            LOGGER.debug("TRUST_REPAIR_EVENT event sent");
            environmentService.validateRepairCrossRealmSetup();
            return EnvironmentCrossRealmTrustRepairEvent.builder()
                    .withSelector(TRUST_REPAIR_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_REPAIR_FAILED event sent");
            return new EnvironmentCrossRealmTrustRepairFailedEvent(data, e, TRUST_REPAIR_FAILED);
        }
    }
}
