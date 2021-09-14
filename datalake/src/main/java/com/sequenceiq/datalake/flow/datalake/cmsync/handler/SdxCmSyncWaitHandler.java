package com.sequenceiq.datalake.flow.datalake.cmsync.handler;

import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FINISHED_EVENT;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncFailedEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncWaitEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitException;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitTaskFactory;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitTaskForCloudbreakFlow;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxCmSyncWaitHandler extends ExceptionCatcherEventHandler<SdxCmSyncWaitEvent> {

    @Value("${sdx.stack.cmsync.sleeptime_sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.cmsync.duration_min}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxWaitTaskFactory sdxWaitTaskFactory;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxCmSyncWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxCmSyncWaitEvent> event) {
        return new SdxCmSyncFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxCmSyncWaitEvent> event) {
        SdxCmSyncWaitEvent sdxCmSyncWaitEvent = event.getData();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxCmSyncWaitEvent.getResourceId());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            SdxWaitTaskForCloudbreakFlow sdxWaitTaskForCloudbreakFlow = sdxWaitTaskFactory.createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, "Sync cm");
            sdxWaitService.waitFor(sdxWaitTaskForCloudbreakFlow);
            return new SdxEvent(SDX_CM_SYNC_FINISHED_EVENT.event(), event.getData().getResourceId(), event.getData().getUserId());
        } catch (SdxWaitException e) {
            return new SdxCmSyncFailedEvent(sdxCmSyncWaitEvent.getResourceId(), sdxCmSyncWaitEvent.getUserId(), e);
        }
    }

}
