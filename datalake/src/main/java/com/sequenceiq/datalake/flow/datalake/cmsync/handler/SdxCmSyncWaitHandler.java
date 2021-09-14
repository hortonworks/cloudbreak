package com.sequenceiq.datalake.flow.datalake.cmsync.handler;

import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FINISHED_EVENT;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncFailedEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncWaitEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxCmSyncWaitHandler extends ExceptionCatcherEventHandler<SdxCmSyncWaitEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCmSyncWaitHandler.class);

    @Value("${sdx.stack.cmsync.sleeptime_sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.cmsync.duration_min}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxCmSyncWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxCmSyncWaitEvent> event) {
        LOGGER.warn("Error happened during syncing CM and parcel versions from a datalake CM to component table {}, error: ", resourceId, e);
        return new SdxCmSyncFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxCmSyncWaitEvent> event) {
        LOGGER.debug("Entering handler for calling sync CM and parcel versions from CM server");
        SdxCmSyncWaitEvent sdxCmSyncWaitEvent = event.getData();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxCmSyncWaitEvent.getResourceId());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster, pollingConfig, "Sync cm");
            return new SdxEvent(SDX_CM_SYNC_FINISHED_EVENT.event(), event.getData().getResourceId(), event.getData().getUserId());
        } catch (SdxWaitException e) {
            LOGGER.warn("Error happened during waiting for syncing CM and parcel versions from a datalake CM to component table {}, error: ",
                    sdxCmSyncWaitEvent.getResourceId(), e);
            return new SdxCmSyncFailedEvent(sdxCmSyncWaitEvent.getResourceId(), sdxCmSyncWaitEvent.getUserId(), e);
        }
    }

}
