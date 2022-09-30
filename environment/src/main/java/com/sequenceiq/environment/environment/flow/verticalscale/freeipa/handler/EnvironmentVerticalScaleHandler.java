package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.handler;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleFailedEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class EnvironmentVerticalScaleHandler extends EventSenderAwareHandler<EnvironmentVerticalScaleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentVerticalScaleHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    protected EnvironmentVerticalScaleHandler(EventSender eventSender, FreeIpaService freeIpaService, FreeIpaPollerService freePollerIpaService) {
        super(eventSender);
        this.freeIpaService = freeIpaService;
        freeIpaPollerService = freePollerIpaService;
    }

    @Override
    public String selector() {
        return VERTICAL_SCALING_FREEIPA_HANDLER.selector();
    }

    @Override
    public void accept(Event<EnvironmentVerticalScaleEvent> verticalScaleFreeIPAEventEvent) {
        LOGGER.debug("In VerticalScaleFreeIPAHandler.accept");
        EnvironmentVerticalScaleEvent environmentVerticalScaleEvent = verticalScaleFreeIPAEventEvent.getData();
        try {
            freeIpaService.describe(environmentVerticalScaleEvent.getResourceCrn()).ifPresentOrElse(freeIpa -> {
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, Vertical Scale interrupted.");
                } else if (!freeIpa.getStatus().isVerticallyScalable()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to Vertically Scale. Current state is: " +
                            freeIpa.getStatus().name());
                } else {
                    LOGGER.info("FreeIPA will be vertically scaled.");
                    freeIpaPollerService.waitForVerticalScale(
                            environmentVerticalScaleEvent.getResourceId(),
                            environmentVerticalScaleEvent.getResourceCrn(),
                            environmentVerticalScaleEvent.getFreeIPAVerticalScaleRequest());
                }

                EnvironmentVerticalScaleEvent result = EnvironmentVerticalScaleEvent.builder()
                        .withSelector(EnvironmentVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_FREEIPA_EVENT.selector())
                        .withResourceCrn(environmentVerticalScaleEvent.getResourceCrn())
                        .withResourceId(environmentVerticalScaleEvent.getResourceId())
                        .withResourceName(environmentVerticalScaleEvent.getResourceName())
                        .withFreeIPAVerticalScaleRequest(environmentVerticalScaleEvent.getFreeIPAVerticalScaleRequest())
                        .build();

                eventSender().sendEvent(result, verticalScaleFreeIPAEventEvent.getHeaders());
                LOGGER.debug("VERTICAL_SCALE_FREEIPA_EVENT event sent");

            }, () -> {
                throw new FreeIpaOperationFailedException(String.format("FreeIPA cannot be found for environment %s",
                        environmentVerticalScaleEvent.getResourceName()));
            });

        } catch (Exception e) {
            EnvironmentVerticalScaleFailedEvent failedEvent =
                    new EnvironmentVerticalScaleFailedEvent(environmentVerticalScaleEvent, e, EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, verticalScaleFreeIPAEventEvent.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_ON_FREEIPA_FAILED event sent");
        }
    }

}
