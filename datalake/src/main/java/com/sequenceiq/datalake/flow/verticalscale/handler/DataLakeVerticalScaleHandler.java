package com.sequenceiq.datalake.flow.verticalscale.handler;

import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_HANDLER;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class DataLakeVerticalScaleHandler extends EventSenderAwareHandler<DataLakeVerticalScaleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeVerticalScaleHandler.class);

    @Value("${sdx.stack.verticalscale.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.verticalscale.duration_min:120}")
    private int durationInMinutes;

    private final VerticalScaleService verticalScaleService;

    protected DataLakeVerticalScaleHandler(EventSender eventSender, VerticalScaleService verticalScaleService) {
        super(eventSender);
        this.verticalScaleService = verticalScaleService;
    }

    @Override
    public String selector() {
        return VERTICAL_SCALING_DATALAKE_HANDLER.selector();
    }

    @Override
    public void accept(Event<DataLakeVerticalScaleEvent> request) {
        LOGGER.debug("In DataLakeVerticalScaleHandler.accept");
        DataLakeVerticalScaleEvent dataLakeVerticalScaleEvent = request.getData();
        try {
            LOGGER.debug("start polling stack creation process for id: {}", dataLakeVerticalScaleEvent.getResourceId());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            verticalScaleService.startVerticalScale(request.getData().getResourceId(), request.getData().getVerticalScaleRequest());
            verticalScaleService.waitCloudbreakClusterVerticalScale(request.getData().getResourceId(), pollingConfig);
            DataLakeVerticalScaleEvent result = DataLakeVerticalScaleEvent.builder()
                    .withSelector(DataLakeVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_DATALAKE_EVENT.selector())
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
            DataLakeVerticalScaleFailedEvent failedEvent =
                    new DataLakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, userBreakException,
                            DatalakeStatusEnum.VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for SDX: {}", dataLakeVerticalScaleEvent.getResourceId(), pollerStoppedException);
            DataLakeVerticalScaleFailedEvent failedEvent =
                    new DataLakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, pollerStoppedException,
                            DatalakeStatusEnum.VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_FREEIPA_FAILED event sent");
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for stack: {}", dataLakeVerticalScaleEvent.getResourceId(), exception);
            DataLakeVerticalScaleFailedEvent failedEvent =
                    new DataLakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, exception,
                            DatalakeStatusEnum.VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_FREEIPA_FAILED event sent");
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in stack creation wait phase", anotherException);
            DataLakeVerticalScaleFailedEvent failedEvent =
                    new DataLakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, anotherException,
                            DatalakeStatusEnum.VERTICAL_SCALE_ON_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, request.getHeaders());
            LOGGER.debug("VERTICAL_SCALE_FREEIPA_FAILED event sent");
        }
    }
}
