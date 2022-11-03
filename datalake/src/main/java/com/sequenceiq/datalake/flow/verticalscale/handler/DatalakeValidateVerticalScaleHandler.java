package com.sequenceiq.datalake.flow.verticalscale.handler;

import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeValidateVerticalScaleHandler extends EventSenderAwareHandler<DatalakeVerticalScaleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeValidateVerticalScaleHandler.class);

    protected DatalakeValidateVerticalScaleHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeVerticalScaleEvent> verticalScaleDataLakeEvent) {
        LOGGER.debug("In ValidateVerticalScaleDataLakeHandler.accept");
        try {
            DatalakeVerticalScaleEvent result = DatalakeVerticalScaleEvent.builder()
                    .withAccepted(new Promise<>())
                    .withSelector(DatalakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_EVENT.selector())
                    .withResourceCrn(verticalScaleDataLakeEvent.getData().getResourceCrn())
                    .withResourceId(verticalScaleDataLakeEvent.getData().getResourceId())
                    .withResourceName(verticalScaleDataLakeEvent.getData().getResourceName())
                    .withStackCrn(verticalScaleDataLakeEvent.getData().getStackCrn())
                    .withVerticalScaleRequest(verticalScaleDataLakeEvent.getData().getVerticalScaleRequest())
                    .build();

            eventSender().sendEvent(result, verticalScaleDataLakeEvent.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_DATALAKE_EVENT event sent");
        } catch (Exception e) {
            DatalakeVerticalScaleFailedEvent failedEvent =
                    new DatalakeVerticalScaleFailedEvent(verticalScaleDataLakeEvent.getData(), e, DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_FAILED);
            eventSender().sendEvent(failedEvent, verticalScaleDataLakeEvent.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_DATALAKE_FAILED event sent with error: {} on stack {}",
                    e.getMessage(),
                    verticalScaleDataLakeEvent.getData().getStackCrn());
        }
    }
}
