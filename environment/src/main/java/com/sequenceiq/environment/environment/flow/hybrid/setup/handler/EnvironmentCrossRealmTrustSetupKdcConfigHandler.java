package com.sequenceiq.environment.environment.flow.hybrid.setup.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_KDC_CONFIG_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_UPDATE_STACKS_EVENT;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;

@Component
public class EnvironmentCrossRealmTrustSetupKdcConfigHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupKdcConfigHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    protected EnvironmentCrossRealmTrustSetupKdcConfigHandler(FreeIpaService freeIpaService, FreeIpaPollerService freeIpaPollerService) {
        this.freeIpaService = freeIpaService;
        this.freeIpaPollerService = freeIpaPollerService;
    }

    @Override
    public String selector() {
        return TRUST_SETUP_KDC_CONFIG_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFailedEvent(event.getData(), e, TRUST_SETUP_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustSetupKdcConfigHandler.accept");
        EnvironmentCrossRealmTrustSetupEvent data = event.getData();
        try {
            Optional<DescribeFreeIpaResponse> freeIpaResponseOptional = freeIpaService.describe(data.getResourceCrn());
            if (freeIpaResponseOptional.isPresent()) {
                DescribeFreeIpaResponse freeIpa = freeIpaResponseOptional.get();
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, KDC configuration interrupted.");
                } else if (!crossRealmTrustKdcConfigCanBeTriggered(freeIpa)) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state for KDC configuration. Current trust state is: " +
                            freeIpa.getTrust().getTrustStatus());
                } else {
                    LOGGER.info("Triggering FreeIPA cross-realm trust KDC configuration.");
                    FinishSetupCrossRealmTrustRequest request = new FinishSetupCrossRealmTrustRequest();
                    request.setEnvironmentCrn(data.getResourceCrn());
                    freeIpaPollerService.waitForCrossRealmFinish(
                            data.getResourceId(),
                            data.getResourceCrn(),
                            request);
                }
            }
            LOGGER.debug("TRUST_SETUP_UPDATE_STACKS_EVENT event sent");
            return data.toBuilder()
                    .withSelector(TRUST_SETUP_UPDATE_STACKS_EVENT.selector())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_SETUP_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFailedEvent(data, e, TRUST_SETUP_FAILED);
        }
    }

    private boolean crossRealmTrustKdcConfigCanBeTriggered(DescribeFreeIpaResponse freeIpa) {
        if (freeIpa.getTrust() != null && StringUtils.isNotBlank(freeIpa.getTrust().getTrustStatus())) {
            TrustStatus trustStatus = TrustStatus.valueOf(freeIpa.getTrust().getTrustStatus());
            return trustStatus.isCrossRealmFinishable();
        }
        return false;
    }
}
