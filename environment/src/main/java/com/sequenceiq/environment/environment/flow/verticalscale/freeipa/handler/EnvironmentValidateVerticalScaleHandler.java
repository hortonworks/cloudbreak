package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.handler;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleFailedEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class EnvironmentValidateVerticalScaleHandler extends EventSenderAwareHandler<EnvironmentVerticalScaleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateVerticalScaleHandler.class);

    protected EnvironmentValidateVerticalScaleHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER.selector();
    }

    @Override
    public void accept(Event<EnvironmentVerticalScaleEvent> verticalScaleFreeIPAEvent) {
        LOGGER.debug("In ValidateVerticalScaleFreeIPAHandler.accept");
        try {
            EnvironmentVerticalScaleEvent result = EnvironmentVerticalScaleEvent.builder()
                    .withAccepted(new Promise<>())
                    .withSelector(EnvironmentVerticalScaleStateSelectors.VERTICAL_SCALING_FREEIPA_EVENT.selector())
                    .withResourceCrn(verticalScaleFreeIPAEvent.getData().getResourceCrn())
                    .withResourceId(verticalScaleFreeIPAEvent.getData().getResourceId())
                    .withResourceName(verticalScaleFreeIPAEvent.getData().getResourceName())
                    .withFreeIPAVerticalScaleRequest(verticalScaleFreeIPAEvent.getData().getFreeIPAVerticalScaleRequest())
                    .build();

            eventSender().sendEvent(result, verticalScaleFreeIPAEvent.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_FREEIPA_EVENT event sent");
        } catch (Exception e) {
            EnvironmentVerticalScaleFailedEvent failedEvent =
                    new EnvironmentVerticalScaleFailedEvent(verticalScaleFreeIPAEvent.getData(), e, EnvironmentStatus.VERTICAL_SCALE_FAILED);
            eventSender().sendEvent(failedEvent, verticalScaleFreeIPAEvent.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_ON_FREEIPA_FAILED event sent");
        }
    }
}
