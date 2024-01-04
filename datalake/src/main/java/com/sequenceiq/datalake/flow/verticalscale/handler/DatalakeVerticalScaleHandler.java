package com.sequenceiq.datalake.flow.verticalscale.handler;

import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_HANDLER;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeVerticalScaleHandler extends EventSenderAwareHandler<DatalakeVerticalScaleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeVerticalScaleHandler.class);

    @Value("${sdx.stack.verticalscale.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.verticalscale.duration_min:120}")
    private int durationInMinutes;

    @Inject
    private VerticalScaleService verticalScaleService;

    protected DatalakeVerticalScaleHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return VERTICAL_SCALING_DATALAKE_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeVerticalScaleEvent> request) {
        LOGGER.debug("In DataLakeVerticalScaleHandler.accept");
        DatalakeVerticalScaleEvent dataLakeVerticalScaleEvent = request.getData();
        try {
            LOGGER.debug("start polling stack creation process for id: {}", dataLakeVerticalScaleEvent.getResourceId());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            verticalScaleService.startVerticalScale(request.getData().getResourceId(), request.getData().getVerticalScaleRequest());
            verticalScaleService.waitCloudbreakClusterVerticalScale(request.getData().getResourceId(), pollingConfig);
            DatalakeVerticalScaleEvent result = DatalakeVerticalScaleEvent.builder()
                    .withSelector(DatalakeVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_DATALAKE_EVENT.selector())
                    .withResourceCrn(dataLakeVerticalScaleEvent.getResourceCrn())
                    .withResourceId(dataLakeVerticalScaleEvent.getResourceId())
                    .withResourceName(dataLakeVerticalScaleEvent.getResourceName())
                    .withVerticalScaleRequest(dataLakeVerticalScaleEvent.getVerticalScaleRequest())
                    .withStackCrn(dataLakeVerticalScaleEvent.getStackCrn())
                    .build();

            eventSender().sendEvent(result, request.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_DATALAKE_EVENT event sent");
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout for SDX: {}. Cause: ", dataLakeVerticalScaleEvent.getResourceId(), userBreakException);
            DatalakeVerticalScaleFailedEvent failedEvent =
                    new DatalakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, userBreakException,
                            DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for SDX: {}", dataLakeVerticalScaleEvent.getResourceId(), pollerStoppedException);
            DatalakeVerticalScaleFailedEvent failedEvent =
                    new DatalakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, pollerStoppedException,
                            DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED event sent");
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for stack: {}", dataLakeVerticalScaleEvent.getResourceId(), exception);
            DatalakeVerticalScaleFailedEvent failedEvent =
                    new DatalakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, exception,
                            DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED event sent");
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in stack creation wait phase", anotherException);
            DatalakeVerticalScaleFailedEvent failedEvent =
                    new DatalakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, anotherException,
                            DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("DATALAKE_VERTICAL_SCALE_ON_DATALAKE_FAILED event sent");
        }
    }

}
