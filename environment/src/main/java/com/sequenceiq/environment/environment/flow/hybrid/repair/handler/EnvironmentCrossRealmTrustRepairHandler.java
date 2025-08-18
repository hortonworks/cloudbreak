package com.sequenceiq.environment.environment.flow.hybrid.repair.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairHandlerSelectors.TRUST_REPAIR_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FINISH_TRUST_REPAIR_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairEvent;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EnvironmentCrossRealmTrustRepairHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustRepairEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustRepairHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    private final EnvironmentService environmentService;

    protected EnvironmentCrossRealmTrustRepairHandler(
        FreeIpaService freeIpaService,
        FreeIpaPollerService freePollerIpaService,
        EnvironmentService environmentService
    ) {
        this.freeIpaService = freeIpaService;
        freeIpaPollerService = freePollerIpaService;
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return TRUST_REPAIR_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustRepairEvent> event) {
        return new EnvironmentCrossRealmTrustRepairFailedEvent(event.getData(), e, TRUST_REPAIR_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustRepairEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustRepairHandler.accept");
        EnvironmentCrossRealmTrustRepairEvent data = event.getData();
        try {
            Optional<DescribeFreeIpaResponse> describe = freeIpaService.describe(data.getResourceCrn());
            if (describe.isPresent()) {
                DescribeFreeIpaResponse freeIpa = describe.get();
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, cross realm trust repair interrupted.");
                } else if (freeIpa.getTrust() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA cross realm not found, repair interrupted.");
                } else {
                    LOGGER.info("FreeIPA cross realm trust repair will be started.");
                    freeIpaPollerService.waitForCrossRealmTrustRepair(data.getResourceId(), data.getResourceCrn());
                }
            }
            LOGGER.debug("FINISH_TRUST_REPAIR_EVENT event sent");
            return EnvironmentCrossRealmTrustRepairEvent.builder()
                    .withSelector(FINISH_TRUST_REPAIR_EVENT.selector())
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
