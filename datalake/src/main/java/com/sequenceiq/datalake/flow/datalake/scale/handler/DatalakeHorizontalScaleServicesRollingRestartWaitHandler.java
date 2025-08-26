package com.sequenceiq.datalake.flow.datalake.scale.handler;

import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleHandlerEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_HANDLER;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent.DatalakeHorizontalScaleFlowEventBuilder;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeHorizontalScaleServicesRollingRestartWaitHandler extends ExceptionCatcherEventHandler<DatalakeHorizontalScaleFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeHorizontalScaleServicesRollingRestartWaitHandler.class);

    @Value("${sdx.datalake.scale.sleeptime-sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.datalake.scale.duration-min:30}")
    private int durationInMinutes;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception exception, Event<DatalakeHorizontalScaleFlowEvent> event) {
        LOGGER.error("Datalake horizontal scale rolling restart failed {}", exception.getMessage());
        return createFailureEvent(resourceId, exception, event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeHorizontalScaleFlowEvent> event) {
        DatalakeHorizontalScaleFlowEvent data = event.getData();
        SdxCluster sdxCluster = sdxService.getById(data.getResourceId());
        Long sdxId = data.getResourceId();
        Selectable response;
        try {
            LOGGER.debug("Start CM polling for services rolling restart process with id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            cloudbreakPoller.pollFlowStateBySdxClusterUntilComplete("Datalake horizontal scaling",
                    sdxCluster, pollingConfig);
            LOGGER.debug("Services Rolling restart finsihed");
            response = DatalakeHorizontalScaleFlowEvent
                    .datalakeHorizontalScaleFlowEventBuilderFactory(event.getData())
                    .setSelector(DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT.selector())
                    .build();
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Services rolling restart polling exited before timeout. Cause: ", userBreakException);
            response = createFailureEvent(sdxId, userBreakException, event.getData());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Services rolling restart poller stopped for stack: {}", sdxId);
            response = createFailureEvent(sdxId,
                    new PollerStoppedException("Services rolling restart timed out after " + durationInMinutes + " minutes"), event.getData());
        } catch (PollerException exception) {
            LOGGER.error("Services rolling restart polling failed for stack: {}", sdxId);
            response = createFailureEvent(sdxId, exception, event.getData());
        }
        return response;
    }

    private Selectable createFailureEvent(Long resourceId, Exception exception, DatalakeHorizontalScaleFlowEvent event) {
        return new DatalakeHorizontalScaleFlowEventBuilder()
                .setSelector(DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector())
                .setResourceId(resourceId)
                .setResourceName(event.getResourceName())
                .setUserId(event.getUserId())
                .setResourceCrn(event.getResourceCrn())
                .setScaleRequest(event.getScaleRequest())
                .setException(exception)
                .setCommandId(event.getCommandId())
                .build();
    }
}
