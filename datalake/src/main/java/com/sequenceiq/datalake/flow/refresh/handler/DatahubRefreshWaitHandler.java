package com.sequenceiq.datalake.flow.refresh.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshFailedEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshWaitEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.refresh.SdxRefreshService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatahubRefreshWaitHandler extends ExceptionCatcherEventHandler<DatahubRefreshWaitEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubRefreshWaitHandler.class);

    @Value("${sdx.datahub.refresh.sleeptime_sec:5}")
    private int sleepTimeInSec;

    @Value("${sdx.datahub.refresh.duration_min:5}")
    private int durationInMinutes;

    @Inject
    private SdxRefreshService sdxRefreshService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatahubRefreshWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatahubRefreshWaitEvent> event) {
        return new DatahubRefreshFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatahubRefreshWaitEvent> event) {
        SdxEvent sdxEvent = event.getData();
        Long sdxId = sdxEvent.getResourceId();
        String userId = sdxEvent.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling datahub refresh process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            sdxRefreshService.waitCloudbreakCluster(sdxId, pollingConfig);
            response = new SdxEvent(DatahubRefreshFlowEvent.DATAHUB_REFRESH_FINISHED_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Datahub refresh polling exited before timeout. Cause: ", userBreakException);
            response = new DatahubRefreshFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Datahub refresh poller stopped for stack: {}", sdxId);
            response = new DatahubRefreshFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datahub refresh timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Datahub refresh polling failed for stack: {}", sdxId);
            response = new DatahubRefreshFailedEvent(sdxId, userId, exception);
        }
        return response;
    }

}
