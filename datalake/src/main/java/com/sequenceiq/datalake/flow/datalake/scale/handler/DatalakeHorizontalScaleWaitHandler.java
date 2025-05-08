package com.sequenceiq.datalake.flow.datalake.scale.handler;

import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleHandlerEvent.DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent.DatalakeHorizontalScaleFlowEventBuilder;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeHorizontalScaleWaitHandler extends EventSenderAwareHandler<DatalakeHorizontalScaleFlowEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeHorizontalScaleWaitHandler.class);

    @Value("${sdx.datalake.scale.sleeptime-sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.datalake.scale.duration-min:30}")
    private int durationInMinutes;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private SdxService sdxService;

    protected DatalakeHorizontalScaleWaitHandler(CloudbreakPoller cloudbreakPoller, SdxService sdxService, EventSender eventSender) {
        super(eventSender);
        this.cloudbreakPoller = cloudbreakPoller;
        this.sdxService = sdxService;
    }

    @Override
    public String selector() {
        return DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeHorizontalScaleFlowEvent> event) {
        DatalakeHorizontalScaleFlowEvent data = event.getData();
        SdxCluster sdxCluster = sdxService.getById(data.getResourceId());
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            cloudbreakPoller.pollFlowStateBySdxClusterUntilComplete("Datalake horizontal scaling",
                    sdxCluster, pollingConfig);
        } catch (SdxWaitException e) {
            LOGGER.info("Flow failed. Waiting for Datalake Horizontal timed out.", e);

            DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                    .datalakeHorizontalScaleFlowEventBuilderFactory(data)
                    .setSelector(DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector());
            eventSender().sendEvent(resultEventBuilder.build(), event.getHeaders());
        }
        LOGGER.info("Polling finished for flow. Id: {}", data.getFlowId());
        DatalakeHorizontalScaleFlowEventBuilder resultEventBuilder = DatalakeHorizontalScaleFlowEvent
                .datalakeHorizontalScaleFlowEventBuilderFactory(data)
                .setSelector(DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_EVENT.selector());
        eventSender().sendEvent(resultEventBuilder.build(), event.getHeaders());
    }
}
