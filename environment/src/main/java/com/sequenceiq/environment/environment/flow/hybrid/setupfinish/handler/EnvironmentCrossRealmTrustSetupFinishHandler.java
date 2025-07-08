package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINISH_TRUST_SETUP_FINISH_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EnvironmentCrossRealmTrustSetupFinishHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupFinishEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupFinishHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    protected EnvironmentCrossRealmTrustSetupFinishHandler(FreeIpaService freeIpaService, FreeIpaPollerService freePollerIpaService) {
        this.freeIpaService = freeIpaService;
        this.freeIpaPollerService = freePollerIpaService;
    }

    @Override
    public String selector() {
        return SETUP_FINISH_TRUST_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(event.getData(), e, TRUST_SETUP_FINISH_VALIDATION_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        LOGGER.debug("In EnvironmentSetupFinishCrossRealmTrustHandler.accept");
        EnvironmentCrossRealmTrustSetupFinishEvent data = event.getData();
        try {
            Optional<DescribeFreeIpaResponse> freeIpaResponseOptional = freeIpaService.describe(data.getResourceCrn());
            if (freeIpaResponseOptional.isPresent()) {
                DescribeFreeIpaResponse freeIpa = freeIpaResponseOptional.get();
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, Setup finish interrupted.");
                } else if (!freeIpa.getStatus().isCrossRealmFinishable()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to Finish Cross Realm setup. Current state is: " +
                            freeIpa.getStatus().name());
                } else {
                    LOGGER.info("FreeIPA Cross Realm Trust setup finished.");
                    FinishSetupCrossRealmTrustRequest finishCrossRealmTrustRequest = new FinishSetupCrossRealmTrustRequest();
                    finishCrossRealmTrustRequest.setEnvironmentCrn(data.getResourceCrn());
                    freeIpaPollerService.waitForCrossRealmFinish(
                            data.getResourceId(),
                            data.getResourceCrn(),
                            finishCrossRealmTrustRequest);
                }
            }
            LOGGER.debug("FINISH_TRUST_SETUP_FINISH_EVENT event sent");
            return EnvironmentCrossRealmTrustSetupFinishEvent.builder()
                    .withSelector(FINISH_TRUST_SETUP_FINISH_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("FINISH_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(data, e, TRUST_SETUP_FINISH_FAILED);
        }
    }

}
