package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINISH_TRUST_CANCEL_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EnvironmentCrossRealmTrustCancelHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustCancelEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustCancelHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    private final EnvironmentService environmentService;

    protected EnvironmentCrossRealmTrustCancelHandler(
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
        return TRUST_CANCEL_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustCancelEvent> event) {
        return new EnvironmentCrossRealmTrustCancelFailedEvent(event.getData(), e, TRUST_CANCEL_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustCancelHandler.accept");
        EnvironmentCrossRealmTrustCancelEvent data = event.getData();
        try {
            environmentService.removeRemoteEnvironmentCrn(data.getResourceCrn());

            Optional<DescribeFreeIpaResponse> describe = freeIpaService.describe(data.getResourceCrn());
            if (describe.isPresent()) {
                DescribeFreeIpaResponse freeIpa = describe.get();
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, cross realm trust cancel interrupted.");
                } else {
                    LOGGER.info("FreeIPA will be cross realm trust cancel.");
                    freeIpaPollerService.waitForCrossRealmTrustCancel(data.getResourceId(), data.getResourceCrn());
                }
            }
            LOGGER.debug("FINISH_TRUST_CANCEL_EVENT event sent");
            return EnvironmentCrossRealmTrustCancelEvent.builder()
                    .withSelector(FINISH_TRUST_CANCEL_EVENT.selector())
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
